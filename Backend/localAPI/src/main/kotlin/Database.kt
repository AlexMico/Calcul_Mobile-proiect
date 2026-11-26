package com.example

import kotlinx.serialization.descriptors.PrimitiveKind
import java.sql.DriverManager
import java.sql.Connection
import java.sql.SQLException
import java.time.LocalDateTime

data class capturaData(val idCaptura: Int, val greutate:Double, val lungime:Double, val dataCaptura: LocalDateTime,val specie:String, val momeala:String, val tehnica: String, val nume_locatie: String)
data class addCaptures(val greutate:Double, val lungime:Double, val data_captura: LocalDateTime, val specie:String, val momeala: String, val tehnica: String, val google_place_id: String, val nume_locatie: String, val adresa_comfirmata: String)
data class userData(val id: Int,val email:String, val username:String, val rol:String)
data class locatieData(val google_place_id:String, val nume_locatie:String, val adresa_confirmata:String, val scor:Double, val statistica:Statistica)
data class Statistica(val nrCapturi: Int, val greutateMedie: Double, val lungimeMedie:Double, val momeala: String, val specii: List<String>)

object Database {
    fun testConnection(): Boolean {
        val url = "jdbc:mysql://localhost:3306/FishingDB" // pune numele bazei tale
        val user = "Adi"                               // utilizator MySQL
        val password = "FishingTracker123"                      // parola MySQL

        return try {
            DriverManager.getConnection(url, user, password).use { conn ->
                println("Conexiunea la MySQL a fost realizata cu succes!")
                true
            }
        } catch (e: Exception) {
            println("Nu s-a putut conecta la MySQL.")
            e.printStackTrace()
            false
        }
    }

    fun getConnection(): Connection{
        val url = "jdbc:mysql://localhost:3306/FishingDB"
        val user = "Adi"
        val password = "FishingTracker123"

        return DriverManager.getConnection(url,user,password)
    }
}

//1 select
fun getUserId(email: String, password: String): Int? {
    Database.getConnection().use { conn ->
        //luam id ul userului pentru token
        val stmt = conn.prepareStatement(
            "SELECT id FROM Users WHERE email = ? AND password = ?"
        )
        stmt.setString(1, email)
        stmt.setString(2, password)
        val rs = stmt.executeQuery()

        return if (rs.next()) {
            rs.getInt("id")
        } else {
            null
        }
    }
}

//1 select
fun emailExists(email: String): Boolean {
    Database.getConnection().use { conn ->
        //verificam daca exista deja userul
        val stmt = conn.prepareStatement(
            "SELECT * FROM Users WHERE email = ?"
        )
        stmt.setString(1, email)

        val rs = stmt.executeQuery()

        return rs.next()
    }
}

//1 insert
fun addUser(email: String, password: String, username: String, rol: String): Boolean {
    Database.getConnection().use { conn ->
        //adaugam un nou user
        val stmt = conn.prepareStatement(
            "INSERT INTO Users (email, password, username, rol) VALUES (?,?,?,?)"
        )

        stmt.setString(1, email)
        stmt.setString(2, password)
        stmt.setString(3, username)
        stmt.setString(4, rol)//rolul va fi pus default "user" la register

        val rs = stmt.executeUpdate()
        return rs>0
    }
}

//2 delete
fun deleteUser(userId: Int){
    Database.getConnection().use { conn ->
        conn.autoCommit = false
        try {
            val deleteCapturi= conn.prepareStatement(
                //stergem toate capturile inregistrate de utilizator
                "DELETE FROM Captura WHERE id_utilizator=?"
            )
            deleteCapturi.setInt(1, userId)
            deleteCapturi.executeUpdate()

            val deleteUser= conn.prepareStatement(
                //stergem utilizatorul
                "DELETE FROM Users WHERE id = ?"
            )
            deleteUser.setInt(1, userId)
            deleteUser.executeUpdate()

            conn.commit()
        }
        catch (e: SQLException) {
            conn.rollback()
            e.printStackTrace()
        }
        finally {
            conn.autoCommit = true
        }

    }
}

//1 select cu orderby
fun getListUsers():MutableList<userData>{
    val userList= mutableListOf<userData>()
    Database.getConnection().use { conn ->
        try {
            val getUsers = conn.prepareStatement(
                "SELECT id, email, username, rol FROM Users ORDER BY data_creare DESC"
            )

            val rs=getUsers.executeQuery()

            while (rs.next()) {
                val user= userData(
                    id=rs.getInt("id"),
                    email=rs.getString("email"),
                    username=rs.getString("username"),
                    rol=rs.getString("rol")
                )
                userList.add(user)
            }

        }
        catch (e:Exception){
            e.printStackTrace()
        }

    }
    return userList
}

//1 update
fun modifyUser(userId: Int, email: String, username:String)
{
    Database.getConnection().use { conn ->
        val stmt = conn.prepareStatement(
            "UPDATE Users SET email=?, username=? WHERE id=?"
        )
        stmt.setString(1, email)
        stmt.setString(2, username)
        stmt.setInt(3,userId)
        stmt.executeUpdate()
    }
}

//13 (insert+select) + 1 ON DUPLICATE UPDATE
fun addCaptura(userId: Int,data:addCaptures) {
    Database.getConnection().use { conn ->

        //dezactivam salvarea automata ca sa gestionam manual tranzactia
        conn.autoCommit = false

        try {

            //inseram locatia aleasa
            conn.prepareStatement("INSERT IGNORE INTO Locatie(google_place_id, nume_locatie, adresa_comfirmata) VALUES (?,?,?)").apply {
                setString(1, data.google_place_id)
                setString(2, data.nume_locatie)
                setString(3, data.adresa_comfirmata)
                executeUpdate()
            }

            //inseram specia
            conn.prepareStatement("INSERT IGNORE INTO Specie_peste(nume_specie) VALUES (?)").apply {
                setString(1, data.specie)
                executeUpdate()
            }

            //inseram momeala
            conn.prepareStatement("INSERT IGNORE INTO Momeala(nume) VALUES (?)").apply {
                setString(1, data.momeala)
                executeUpdate()
            }
            //inseram tehnica de pescuit
            conn.prepareStatement("INSERT IGNORE INTO Tehnica_pescuit(nume) VALUES (?)").apply {
                setString(1, data.tehnica)
                executeUpdate()
            }


            //inseram captura atribuind datele inserate mai sus
            val insertCaptura="""
            INSERT INTO Captura(greutate, lungime, data_captura, id_utilizator, id_locatie, id_specie, id_momeala, id_tehnica) 
            VALUES (?,?,?,?,
            (SELECT id_locatie FROM Locatie WHERE google_place_id=? LIMIT 1),
            (SELECT id_specie FROM Specie_peste WHERE nume_specie=? LIMIT 1),
            (SELECT id_momeala FROM Momeala WHERE nume_momeala=? LIMIT 1),
            (SELECT id_tehnica FROM Tehnica_pescuit WHERE nume_tehnica=? LIMIT 1)
            )
        """.trimIndent()

            conn.prepareStatement(insertCaptura).apply {
                setDouble(1,data.greutate)
                setDouble(2,data.lungime)
                setTimestamp(3,java.sql.Timestamp.valueOf(data.data_captura))
                setInt(4,userId)
                setString(5,data.google_place_id)
                setString(6,data.specie)
                setString(7,data.momeala)
                setString(8,data.tehnica)
                executeUpdate()
            }

            //aici facem sau dam update la statistica
            val updateStatistica=
                    """
                        INSERT INTO Statistica_locatie (id_locatie, nr_capturi, greutate_medie, lungime_medie, top_specii, top_momeala)
                        VALUES (            
                            (SELECT id_locatie FROM Locatie WHERE google_place_id=? LIMIT 1),
                            1,?,?,?,?
                            )
                        ON DUPLICATE KEY UPDATE
                            greutate_medie=(greutate_medie*nr_capturi + VALUES(greutate_medie))/(nr_capturi+1),
                            lungime_medie=(lungime_medie*nr_capturi + VALUES(lungime_medie))/(nr_capturi+1),
                            nr_capturi=nr_capturi+1,
                            top_specii = (
                                SELECT GROUP_CONCAT(sub.nume_specie SEPARATOR ', ')
                                FROM (
                                    SELECT S.nume_specie
                                    FROM Captura C
                                    INNER JOIN Specie_peste S ON C.id_specie = S.id_specie
                                    WHERE C.id_locatie = (SELECT id_locatie FROM Locatie WHERE google_place_id=? LIMIT 1)
                                    GROUP BY S.id_specie
                                    ORDER BY COUNT(*) DESC
                                    LIMIT 4
                                    ) AS sub
                                ),
                                top_momeala = (
                                    SELECT M.nume_momeala 
                                    FROM Captura C 
                                    INNER JOIN Momeala M ON C.id_momeala = M.id_momeala 
                                    WHERE C.id_locatie = (SELECT id_locatie FROM Locatie WHERE google_place_id=? LIMIT 1) 
                                    GROUP BY C.id_momeala 
                                    ORDER BY COUNT(*) DESC 
                                    LIMIT 1
                                )
),
                        
                    """.trimIndent()
            //am folosit subquery uri la group_concat ca sa pot sa folosesc limit 4 ca sa imi ia cele 4 specii cele mai frecvent prinse

            conn.prepareStatement(updateStatistica).apply {
                setString(1, data.google_place_id)
                setString(2,data.specie)
                setString(3,data.momeala)
                setDouble(4,data.greutate)
                setDouble(5,data.lungime)
                setString(6, data.google_place_id)//pentru subquery ul pt specii
                setString(7, data.google_place_id)//pt subquery ul pt momeli
                executeUpdate()
            }

            //dam comit la tranzactie
            conn.commit()
        }
        catch (e: SQLException) {
            //daca apare vreo eroare anulam tot ce am trimis
            conn.rollback()
            e.printStackTrace()
        }
        finally {
            //daca nu sunt erori resetam conexiunea
            conn.autoCommit = true
        }

    }
}

//1 delete
fun deleteCaptura(idCaptura: Int) {
    Database.getConnection().use { conn ->
        val stmt = conn.prepareStatement(
            "DELETE FROM Captura WHERE id=?" //stergem doar ce este in tabela captura pentru ca vreau sa ramana inregistrate spciile, momelile si locatiile
        )
        stmt.setInt(1, idCaptura)

        val affectedRows = stmt.executeUpdate()
        if (affectedRows > 0) {
            println("Captura a fost stearsa")
        }

    }
}

// 1 select, 4 INNER JOIN, 1 order
fun getListaCapturi(userId: Int): MutableList<capturaData>? {
    val listaCapturi= mutableListOf<capturaData>()
    Database.getConnection().use { conn ->

        try {
            val captura=(
                """
                    SELECT C.id_captura, C.greutate, C.lungime, C.data_captura, S.specie, L.locatie, M.momeala, T.tehnica
                    FROM Captura C
                    INNER JOIN Specie_peste S ON S.id_specie=C.id_specie
                    INNER JOIN Locatie L ON L.id_locatie=C.id_locatie
                    INNER JOIN Momeala M ON M.id_momeala=C.id_momeala
                    INNER JOIN Tehnica T ON T.tehnica=C.id_tehnica
                    WHERE C.id_utilizator=?
                    ORDER BY data_captura DESC
            """.trimIndent()
            )

            val stm=conn.prepareStatement(captura)
            stm.setInt(1, userId)
            val rs = stm.executeQuery()

            while (rs.next()) {
                val cap= capturaData(
                    idCaptura = rs.getInt("id_captura"),
                    greutate = rs.getDouble("greutate"),
                    lungime = rs.getDouble("lungime"),
                    dataCaptura= rs.getTimestamp("data_captura").toLocalDateTime(),
                    specie = rs.getString("nume_specie"),
                    nume_locatie = rs.getString("nume_locatie"),
                    momeala = rs.getString("nume_momeala"),
                    tehnica = rs.getString("nume_tehnica"),

                  )

                listaCapturi.add(cap)
            }
        }
        catch (e: SQLException) {
            e.printStackTrace()
        }

    }
    return listaCapturi
}

//1select + 1 inner join
fun getListaLocatii(): MutableList<locatieData>?{

    val listaLocatii= mutableListOf<locatieData>()

    Database.getConnection().use { conn ->
        try {
            val stm=conn.prepareStatement(
                """
                    SELECT L.id_locatie,L.google_place_id, L.nume_locatie, L.adresa_confirmata, L.scor, ST.nr_capturi, ST.greutate_medie, ST.lungime _medie, ST.top_specii, ST.top_momeala
                    FROM Locatie L 
                    INNER JOIN Statistica_locatie ST ON ST.id_locatie=L.id_locatie
                """.trimIndent()
            )
            val rs = stm.executeQuery()
            while (rs.next()) {
                val stats= Statistica(
                    nrCapturi = rs.getInt("nr_capturi"),
                    greutateMedie = rs.getDouble("greutate_medie"),
                    lungimeMedie = rs.getDouble("lungime_medie"),
                    momeala = rs.getString("top_momeala"),
                    specii = rs.getString("top_specii").split(',')?: emptyList()

                )
                val locatie = locatieData(
                    google_place_id = rs.getString("google_place_id"),
                    nume_locatie = rs.getString("nume_locatie"),
                    adresa_confirmata = rs.getString("adresa_comfirmata"),
                    scor = rs.getDouble("scor"),
                    statistica = stats
                )

                listaLocatii.add(locatie)
            }
        }
        catch (e:SQLException) {
            e.printStackTrace()
            return null
        }
    }
    return listaLocatii
}
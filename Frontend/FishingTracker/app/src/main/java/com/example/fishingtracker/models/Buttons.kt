package com.example.fishingtracker.models

import android.content.Context
import android.content.Intent
import android.service.autofill.OnClickAction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.fishingtracker.Login
import com.example.fishingtracker.MapWindow
import com.example.fishingtracker.Register

@Composable
fun normalButton(text: String, onClick: ()-> Unit)
{
    Button(
        onClick=onClick,
        modifier=Modifier.width(200.dp)) {

        Text(text)
    }
}

@Composable
fun textButton(text:String, onClick: () -> Unit){

    TextButton(
        onClick=onClick
    ) {
        Text(text)
    }
}
class Buttons (private val context: Context) {


    @Composable
    fun LoginTextButton(context: Context){
        TextButton(onClick = {
            val intent= Intent(context, Login::class.java).apply{

            }
            context.startActivity(intent)

        }
        ) {
            Text("Login")
        }
    }

    @Composable
    fun RegisterTextButton(context: Context){

        TextButton(onClick = {
            val intent= Intent(context, Register::class.java).apply{

            }
            context.startActivity(intent)
        })
        {
            Text("Register")
        }
    }

    @Composable
    fun RegisterButton(context: Context){

        Button(onClick = {
            val intent= Intent(context, Login::class.java).apply{

            }
            context.startActivity(intent)
        })
        {
            Text("Register")
        }
    }
}
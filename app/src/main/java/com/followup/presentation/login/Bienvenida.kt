package com.followup.presentation.login

/* ----------------------------------------------------------------------------------------
                                           IMPORTS
---------------------------------------------------------------------------------------- */

import android.content.Intent
    // Permite navegar entre Activities (cambiar de pantalla).
    // Se usa para abrir Login desde Bienvenida.

import android.os.Bundle
    // Contiene datos del estado de la Activity.
    // Se usa en onCreate para recuperar información si la app se recrea.

import androidx.activity.enableEdgeToEdge
    // Permite que tu UI use toda la pantalla (debajo del status bar y nav bar).

import androidx.appcompat.app.AppCompatActivity
    // Clase base para Activities modernas con compatibilidad.
    // Tu Activity (Bienvenida) hereda de esta.

import androidx.core.view.ViewCompat
    // Utilidades para manejar vistas de forma compatible con distintas versiones de Android.

import androidx.core.view.WindowInsetsCompat
    // Maneja los "insets" del sistema (barra superior, inferior, etc).

import com.followup.R
    // Acceso a recursos de tu app (layouts, ids, drawables, strings, etc).
    // Ej: R.layout.activity_bienvenida

import androidx.lifecycle.lifecycleScope
    // Permite lanzar corrutinas ligadas al ciclo de vida de la Activity.
    // Evita leaks o ejecuciones cuando la Activity ya no existe.

import kotlinx.coroutines.delay
    // Función para pausar una corrutina sin bloquear el hilo principal.

import kotlinx.coroutines.launch
    // Permite iniciar una corrutina (dentro de lifecycleScope).

/* ----------------------------------------------------------------------------------------
                                    ACTIVITY BIENVENIDA
---------------------------------------------------------------------------------------- */
/*
    [+] - Se ejecuta al iniciar la aplicación
*/

class Bienvenida : AppCompatActivity() {

    // Punto de entrada, se ejecuta cuando se crea el "Activity"
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Permite que se use toda la pantalla (Detrás del StatusBar)
        setContentView(R.layout.activity_bienvenida) // Establece el "Layout" del "Activity"

        // Ajusta los margenes del "Layout" del "Activity"
            // Para que no tape contenido y no se superponga con la barra de notificaciones
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()) // Obtiene el tamaño de la barra superior e inferior
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom) // Aplica el anterior tamaño como "Padding"
            insets
        }

        /* -------------------- Ejecuta Código Despues de "X" Tiempo -------------------- */
            // [+] - Transición automática a Login después de 2 segundos. Usando Handler.
            // [+] - Inicia una corrutina (Una tarea asíncrona liviana)
            // [+] - Ligada al ciclo de vida de la Activity (Bienvenida).
            // [+] - Si la Activity se destruye, esto se cancela automáticamente.

        lifecycleScope.launch {

            kotlinx.coroutines.delay(2000)
                // Pausa la ejecución durante 2000 milisegundos (2 segundos)
                // SIN bloquear el hilo principal (no congela la app).

            startActivity(Intent(this@Bienvenida, Login::class.java))
                // Crea un Intent para abrir la Activity "Login"
                // La inicia (Navega a la pantalla de login).

            finish()
                // Cierra esta Activity (Bienvenida)
                // para que el usuario no pueda volver atrás con el botón "back".
        }

    }

}
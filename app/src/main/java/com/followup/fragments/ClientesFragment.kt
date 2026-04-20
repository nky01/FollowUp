package com.followup.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.followup.R
import com.followup.data.adapter.ClienteAdapter
import com.followup.data.database.AppDatabase
import com.followup.data.entity.Cliente
import com.followup.presentation.settings.Configuracion
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.hdodenhof.circleimageview.CircleImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ClientesFragment : Fragment() {
// lateinit es una promesa de que la variable se inicializará antes de su uso.
    private lateinit var fabMain: FloatingActionButton // Referencia al botón flotante principal
    private lateinit var fabMenuContainer: View // Contenedor del menú del botón flotante
    private lateinit var fabOverlay: View // Vista de superposición para oscurecer el fondo cuando el menú está abierto
    private var isFabMenuOpen = false // Bandera para controlar el estado del menú del botón flotante

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClienteAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_clientes, container, false)
    }

    //
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Conectar RecyclerView
        recyclerView = view.findViewById(R.id.rvClientes)

        // Conectar Adapter
        adapter = ClienteAdapter(object : ClienteAdapter.OnClienteClickListener {

            // Hace el clic en el botón de eliminar y muestra un dialogo de confirmacion
            override fun onDeleteClick(cliente: Cliente) {
                mostrarDialogoEliminar(cliente)
            }

            // Hace el clic en el botón de editar y muestra un dialogo de edicion
            override fun onEditClick(cliente: Cliente) {
                mostrarDialogoEditar(cliente)
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val delete = view.findViewById<ImageView>(R.id.btnEliminar)

        // BASE DE DATOS
        val db = AppDatabase.getDatabase(requireContext())

        db.clienteDao().obtenerClientes().observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }

        val filterGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.filter_group)

        filterGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            // Cambiar colores
            actualizarEstilosBotones(group, checkedId)

            // Filtrar
            when (checkedId) {

                R.id.btn_filter_todos -> {
                    adapter.filtrar("TODOS")
                }

                R.id.btn_filter_vendidos -> {
                    adapter.filtrar("Vendido")
                }

                R.id.btn_filter_pendiente -> {
                    adapter.filtrar("Pendiente")
                }

                R.id.btn_filter_no_asignado -> {
                    adapter.filtrar("No Asignado")
                }
            }
        }

        fabMain = view.findViewById(R.id.fab_main)
        fabMenuContainer = view.findViewById(R.id.fab_menu_container)
        fabOverlay = view.findViewById(R.id.fab_overlay)
        val btnNuevoCliente = view.findViewById<MaterialButton>(R.id.btn_nuevo_cliente)
        val profilePicture = view.findViewById<CircleImageView>(R.id.iv_profile_picture)

        fabMain.setOnClickListener { toggleFabMenu() }
        fabOverlay.setOnClickListener { closeFabMenu() }

        btnNuevoCliente.setOnClickListener {
            closeFabMenu()
            showNuevoClienteDialog()
        }

        profilePicture.setOnClickListener {
            startActivity(Intent(requireContext(), Configuracion::class.java))
        }
    }

    // toggleFabMenu se encarga de alternar entre abrir y cerrar el btn flotante
    private fun toggleFabMenu() {
        if (isFabMenuOpen) {
            closeFabMenu()
        } else {
            openFabMenu()
        }
    }

    // openFabMenu se encarga de mostrar el menu del btn flotante con animaciones,
    private fun openFabMenu() {
        isFabMenuOpen = true
        fabOverlay.visibility = View.VISIBLE
        fabMenuContainer.visibility = View.VISIBLE

        fabOverlay.alpha = 0f
        fabOverlay.animate().alpha(1f).setDuration(150).start()

        fabMenuContainer.alpha = 0f
        fabMenuContainer.translationY = 20f
        fabMenuContainer.animate().alpha(1f).translationY(0f).setDuration(180).start()

        fabMain.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
    }

    // closeFabMenu se encarga de ocultar el menu del btn flotante con animaciones
    private fun closeFabMenu() {
        if (!isFabMenuOpen) return

        isFabMenuOpen = false
        fabOverlay.animate().alpha(0f).setDuration(120).withEndAction {
            fabOverlay.visibility = View.GONE
        }.start()

        fabMenuContainer.animate().alpha(0f).translationY(20f).setDuration(140).withEndAction {
            fabMenuContainer.visibility = View.GONE
        }.start()

        fabMain.setImageResource(android.R.drawable.ic_input_add)
    }

    // showNuevoClienteDialog se encarga de mostrar un dialogo personalizado para registrar un nuevo cliente, con validaciones y guardado en la base de datos
    private fun showNuevoClienteDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_nuevo_cliente)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tilNombre = dialog.findViewById<TextInputLayout>(R.id.til_cliente_nombre)
        val tietNombre = dialog.findViewById<TextInputEditText>(R.id.tiet_cliente_nombre)
        val tilTelefono = dialog.findViewById<TextInputLayout>(R.id.til_cliente_telefono)
        val tietTelefono = dialog.findViewById<TextInputEditText>(R.id.tiet_cliente_telefono)
        val tilEmail = dialog.findViewById<TextInputLayout>(R.id.til_cliente_email)
        val tietEmail = dialog.findViewById<TextInputEditText>(R.id.tiet_cliente_email)
        val tilEstado = dialog.findViewById<TextInputLayout>(R.id.til_cliente_estado)
        val actvEstado = dialog.findViewById<AutoCompleteTextView>(R.id.actv_cliente_estado)
        val tietDescripcion = dialog.findViewById<TextInputEditText>(R.id.tiet_cliente_descripcion)
        val btnCancelar = dialog.findViewById<MaterialButton>(R.id.btn_cancelar_cliente)
        val btnGuardar = dialog.findViewById<MaterialButton>(R.id.btn_guardar_cliente)

        val estados = listOf("Pendiente", "Vendido", "No Asignado")
        actvEstado.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, estados))

        btnCancelar.setOnClickListener { dialog.dismiss() }
        btnGuardar.setOnClickListener {
            val nombre = tietNombre.text.toString().trim()
            val telefono = tietTelefono.text.toString().trim()
            val email = tietEmail.text.toString().trim()
            val estado = actvEstado.text.toString().trim()
            val descripcion = tietDescripcion.text.toString().trim()

            if (validarClienteFrontend(nombre, telefono, email, estado, tilNombre, tilTelefono, tilEmail, tilEstado)) {
                registrarCliente(nombre, telefono, email, estado, descripcion, dialog, tilEmail)
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // validarClienteFrontend se encarga de validar los campos del formulario de nuevo cliente, mostrando errores en los TextInputLayout correspondientes
    private fun validarClienteFrontend(
        nombre: String,
        telefono: String,
        email: String,
        estado: String,
        tilNombre: TextInputLayout,
        tilTelefono: TextInputLayout,
        tilEmail: TextInputLayout,
        tilEstado: TextInputLayout
    ): Boolean {
        var esValido = true

        if (nombre.isEmpty()) {
            tilNombre.error = "El nombre es obligatorio"
            esValido = false
        } else {
            tilNombre.error = null
        }

        if (telefono.isEmpty()) {
            tilTelefono.error = "El telefono es obligatorio"
            esValido = false
        } else if (telefono.length < 8) {
            tilTelefono.error = "Ingrese un telefono valido"
            esValido = false
        } else {
            tilTelefono.error = null
        }

        if (email.isEmpty()) {
            tilEmail.error = "El email es obligatorio"
            esValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Formato de email invalido"
            esValido = false
        } else {
            tilEmail.error = null
        }

        if (estado.isEmpty()) {
            tilEstado.error = "Seleccione un estado"
            esValido = false
        } else {
            tilEstado.error = null
        }

        return esValido
    }

    // registrarCliente se encarga de guardar el nuevo cliente en la base de datos, verificando que el email no exista previamente, y mostrando mensajes de exito o error
    private fun registrarCliente(
        nombre: String,
        telefono: String,
        email: String,
        estado: String,
        descripcion: String,
        dialog: Dialog,
        tilEmail: TextInputLayout
    ) {
        lifecycleScope.launch {
            try {
                // Verificar si el email ya existe en la base de datos
                val database = AppDatabase.getDatabase(requireContext())
                val dao = database.clienteDao()
                val existe = dao.obtenerPorEmail(email)

                if (existe == null) {
                    dao.insert(
                        Cliente(
                            nombre = nombre,
                            descripcion = descripcion,
                            telefono = telefono,
                            email = email,
                            estado = estado,
                            fecha = System.currentTimeMillis()
                        )
                    )
                    Toast.makeText(requireContext(), "Cliente guardado con exito", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    tilEmail.error = "Este correo ya esta en uso"
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al guardar cliente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoEliminar(cliente: Cliente) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar cliente")
            .setMessage("¿Seguro que deseas eliminar este cliente?")
            .setPositiveButton("Sí") { _, _ ->

                val db = AppDatabase.getDatabase(requireContext())

                // ELIMINAR EL CLIENTE DE LA BASE DE DATOS
                lifecycleScope.launch {
                    db.clienteDao().delete(cliente)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // onDestreoyView lo recomienda usar, por un tema de memoria
    override fun onDestroyView() {
        super.onDestroyView()
        isFabMenuOpen = false
    }

    private fun mostrarDialogoEditar(cliente: Cliente) {

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_cliente, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.show()

        // Referencias
        val nombre = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tiet_cliente_nombre)
        val telefono = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tiet_cliente_telefono)
        val email = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tiet_cliente_email)
        val descripcion = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tiet_cliente_descripcion)
        val estado = dialogView.findViewById<AutoCompleteTextView>(R.id.actv_cliente_estado)

        val btnGuardar = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_guardar_cliente)
        val btnCancelar = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancelar_cliente)

        // Cargar datos actuales
        nombre.setText(cliente.nombre)
        telefono.setText(cliente.telefono)
        email.setText(cliente.email)
        descripcion.setText(cliente.descripcion)

        // Dropdown estado
        val estados = listOf("Pendiente", "En proceso", "Finalizado")

        val adapterEstados = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            estados
        )

        estado.setAdapter(adapterEstados)
        estado.setText(cliente.estado, false)

        // Cancelar
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        // Guardar cambios
        btnGuardar.setOnClickListener {

            val db = AppDatabase.getDatabase(requireContext())

            val clienteActualizado = cliente.copy(
                nombre = nombre.text.toString(),
                telefono = telefono.text.toString(),
                email = email.text.toString(),
                descripcion = descripcion.text.toString(),
                estado = estado.text.toString()
            )

            lifecycleScope.launch {
                db.clienteDao().update(clienteActualizado)
            }

            dialog.dismiss()
        }

    }

    private fun actualizarEstilosBotones(group: MaterialButtonToggleGroup, checkedId: Int) {

        for (i in 0 until group.childCount) {
            val button = group.getChildAt(i) as MaterialButton

            if (button.id == checkedId) {
                // SELECCIONADO (Azul)
                button.setBackgroundColor(resources.getColor(R.color.primary_blue))
                button.setTextColor(resources.getColor(R.color.white))
            } else {
                // NO SELECCIONADO (Blanco)
                button.setBackgroundColor(Color.parseColor("#F0F0F0"))
                button.setTextColor(Color.parseColor("#475467"))
            }
        }
    }

}
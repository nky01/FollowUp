package com.followup.fragments

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.followup.R
import com.followup.data.entity.Venta
import com.followup.data.database.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class VentasFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ventas, container, false)

        val fabAddVenta = view.findViewById<FloatingActionButton>(R.id.fab_add_venta)

        fabAddVenta.setOnClickListener {
            showAgregarVentaDialog()
        }

        return view
    }
    private fun showAgregarVentaDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_agregar_venta, null)

        val tietNombre = dialogView.findViewById<TextInputEditText>(R.id.tiet_cliente_nombre)
        val tietMonto = dialogView.findViewById<TextInputEditText>(R.id.tiet_monto_total)
        val tietPago = dialogView.findViewById<TextInputEditText>(R.id.tiet_pago_total)
        val tietFechaVenta = dialogView.findViewById<TextInputEditText>(R.id.tiet_fecha_venta)
        val tietFechaSeg = dialogView.findViewById<TextInputEditText>(R.id.tiet_fecha_seguimiento)

        val btnGuardar = dialogView.findViewById<Button>(R.id.btn_guardar_cliente)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btn_cancelar_cliente)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelar.setOnClickListener {
            alertDialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            val nombre = tietNombre.text.toString().trim()
            val montoStr = tietMonto.text.toString()
            val pagoStr = tietPago.text.toString()
            val fechaVenta = tietFechaVenta.text.toString()
            val fechaSeguimiento = tietFechaSeg.text.toString()

            if (nombre.isNotEmpty() && fechaVenta.isNotEmpty() && montoStr.isNotEmpty()) {
                val monto = montoStr.toDoubleOrNull() ?: 0.0
                val pago = pagoStr.toDoubleOrNull() ?: 0.0

                val nuevaVenta = Venta(
                    idClienteVenta = 0,
                    clienteNombre = nombre,
                    total = monto,
                    pagoTotal = pago,
                    fecha = fechaVenta,
                    fechaSeguimiento = fechaSeguimiento,
                    estado = if (pago >= monto) "Pagado" else "Pendiente"
                )

                guardarVentaEnBD(nuevaVenta)
                alertDialog.dismiss()
            } else {
                Toast.makeText(context, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            }
        }
        alertDialog.show()
    }

    private fun guardarVentaEnBD(venta: Venta) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                db.ventaDao().insert(venta)

                Toast.makeText(context, "Venta guardada con éxito", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
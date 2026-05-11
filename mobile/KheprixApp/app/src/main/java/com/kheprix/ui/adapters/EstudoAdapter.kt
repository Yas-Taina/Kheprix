package com.kheprix.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kheprix.R

data class EstudoItem(
    val remoteId: Int,
    val localId: Long?,
    val nome: String,
    val createdAt: String,
    val perfil: String,
    val salvosOffline: Boolean,
    val registrosOffline: Int
) {
    val isProprietario: Boolean get() = perfil == "proprietario"
}

class EstudoAdapter(
    private val estudos: List<EstudoItem>,
    private val onItemClick: (EstudoItem) -> Unit,
    private val onDeleteClick: (EstudoItem) -> Unit,
    private val onColaboradoresClick: (EstudoItem) -> Unit,
    private val onSalvarOfflineClick: (EstudoItem) -> Unit,
    private val onLimparOfflineClick: (EstudoItem) -> Unit,
    private val onAtualizarDadosClick: (EstudoItem) -> Unit
) : RecyclerView.Adapter<EstudoAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView         = view.findViewById(R.id.tvEstudoNome)
        val tvData: TextView         = view.findViewById(R.id.tvEstudoData)
        val ivDelete: ImageView      = view.findViewById(R.id.ivDeleteEstudo)
        val ivColaboradores: ImageView = view.findViewById(R.id.ivColaboradores)
        val btnSalvarOffline: Button = view.findViewById(R.id.btnSalvarOffline)
        val btnLimpar: Button        = view.findViewById(R.id.btnLimparArmazenamento)
        val btnAtualizar: Button     = view.findViewById(R.id.btnAtualizarDados)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_estudo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = estudos[position]

        holder.tvNome.text = item.nome
        holder.tvData.text = formatarData(item.createdAt)

        holder.ivColaboradores.visibility =
            if (item.isProprietario) View.VISIBLE else View.GONE

        if (item.salvosOffline) {
            holder.btnSalvarOffline.visibility = View.GONE
            holder.btnLimpar.visibility        = View.VISIBLE
            holder.btnAtualizar.visibility     = View.VISIBLE
        } else {
            holder.btnSalvarOffline.visibility = View.VISIBLE
            holder.btnLimpar.visibility        = View.GONE
            holder.btnAtualizar.visibility     = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.ivDelete.setOnClickListener { onDeleteClick(item) }
        holder.ivColaboradores.setOnClickListener { onColaboradoresClick(item) }
        holder.btnSalvarOffline.setOnClickListener { onSalvarOfflineClick(item) }
        holder.btnLimpar.setOnClickListener { onLimparOfflineClick(item) }
        holder.btnAtualizar.setOnClickListener { onAtualizarDadosClick(item) }
    }

    override fun getItemCount() = estudos.size

    private fun formatarData(iso: String): String {
        return try {
            val parts = iso.substring(0, 10).split("-")
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } catch (e: Exception) {
            iso
        }
    }
}

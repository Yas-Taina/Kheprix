package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityConvitesBinding
import com.kheprix.models.ConviteRecebidoResponse
import kotlinx.coroutines.launch

/**
 * Convites de Colaboração recebidos.
 *
 * Exibe:
 *  - Botão "Realizar Autocadastro em Estudo" → AutocadastroEstudoActivity
 *  - Lista de convites pendentes com botões Aceitar / Recusar
 *
 * API:
 *  GET  /convites                → lista convites recebidos
 *  POST /convites/:token/aceitar → aceita
 *  POST /convites/:token/recusar → recusa
 */
class ConvitesActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityConvitesBinding
    private val convites = mutableListOf<ConviteRecebidoResponse>()
    private lateinit var adapter: ConviteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConvitesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ConviteAdapter(convites,
            onAceitar = { convite -> responderConvite(convite, aceitar = true) },
            onRecusar = { convite -> responderConvite(convite, aceitar = false) }
        )

        binding.rvConvites.layoutManager = LinearLayoutManager(this)
        binding.rvConvites.adapter = adapter

        binding.btnAutocadastro.setOnClickListener {
            startActivity(Intent(this, AutocadastroEstudoActivity::class.java))
        }

        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarConvites()
    }

    override fun onResume() {
        super.onResume()
        carregarConvites()
    }

    private fun carregarConvites() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getConvitesRecebidos(
                    SessionManager.getAuthHeader()
                )
                if (resp.isSuccessful) {
                    // Filtra apenas convites pendentes
                    val pendentes = (resp.body() ?: emptyList())
                        .filter { it.status.equals("pendente", ignoreCase = true) }
                    convites.clear()
                    convites.addAll(pendentes)
                    adapter.notifyDataSetChanged()

                    binding.tvVazio.visibility =
                        if (convites.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (_: Exception) {
                Toast.makeText(this@ConvitesActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun responderConvite(convite: ConviteRecebidoResponse, aceitar: Boolean) {
        lifecycleScope.launch {
            try {
                val token = SessionManager.getAuthHeader()
                val resp  = if (aceitar)
                    RetrofitClient.apiService.aceitarConvite(token, convite.token)
                else
                    RetrofitClient.apiService.recusarConvite(token, convite.token)

                if (resp.isSuccessful) {
                    val msg = if (aceitar) "Convite aceito!" else "Convite recusado"
                    Toast.makeText(this@ConvitesActivity, msg, Toast.LENGTH_SHORT).show()
                    carregarConvites()
                } else {
                    Toast.makeText(this@ConvitesActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@ConvitesActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// ── Adapter ──────────────────────────────────────────────────────────────────

class ConviteAdapter(
    private val items: List<ConviteRecebidoResponse>,
    private val onAceitar: (ConviteRecebidoResponse) -> Unit,
    private val onRecusar: (ConviteRecebidoResponse) -> Unit
) : RecyclerView.Adapter<ConviteAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNomeEstudo: TextView  = view.findViewById(R.id.tvConviteEstudo)
        val tvRemetente: TextView   = view.findViewById(R.id.tvConviteRemetente)
        val btnAceitar: Button      = view.findViewById(R.id.btnAceitar)
        val btnRecusar: Button      = view.findViewById(R.id.btnRecusar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_convite, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNomeEstudo.text = item.nomeEstudo
        holder.tvRemetente.text  = item.nomeRemetente
        holder.btnAceitar.setOnClickListener { onAceitar(item) }
        holder.btnRecusar.setOnClickListener { onRecusar(item) }
    }

    override fun getItemCount() = items.size
}

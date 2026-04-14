package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityCampanhasBinding
import com.kheprix.models.CampanhaResponse
import kotlinx.coroutines.launch

/**
 * Lista de Campanhas de um Estudo.
 *
 * Extras recebidos:
 *   estudo_remote_id → Int
 *   estudo_nome      → String
 *
 * Ações:
 *  - "Adicionar Campanha" → NovaCampanhaActivity (a criar)
 *  - Clique num item → detalhe/edição da campanha (a criar)
 *  - Lixeira → deletar campanha (com confirmação)
 */
class CampanhasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCampanhasBinding
    private var estudoRemoteId = -1
    private var estudoNome     = ""
    private val campanhas = mutableListOf<CampanhaResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCampanhasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoNome     = intent.getStringExtra("estudo_nome") ?: ""

        binding.tvEstudoNome.text = estudoNome

        setupRecyclerView()
        setupListeners()
        carregarCampanhas()
    }

    override fun onResume() {
        super.onResume()
        carregarCampanhas()
    }

    private fun setupRecyclerView() {
        binding.rvCampanhas.layoutManager = LinearLayoutManager(this)
        binding.rvCampanhas.adapter = CampanhaAdapter(campanhas,
            onItemClick = { campanha ->
                val intent = Intent(this, UnidadesActivity::class.java)
                intent.putExtra("estudo_remote_id", estudoRemoteId)
                intent.putExtra("estudo_nome", estudoNome)
                intent.putExtra("campanha_id", campanha.id)
                intent.putExtra("campanha_nome", campanha.nome)
                startActivity(intent)
            },
            onDeleteClick = { campanha -> confirmarDelete(campanha) }
        )
    }

    private fun setupListeners() {
        binding.btnAdicionarCampanha.setOnClickListener {
            val intent = Intent(this, NovaCampanhaActivityV2::class.java)
            intent.putExtra("estudo_remote_id", estudoRemoteId)
            intent.putExtra("estudo_nome", estudoNome)
            startActivity(intent)
        }
        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    private fun carregarCampanhas() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getCampanhas(
                    SessionManager.getAuthHeader(), estudoRemoteId
                )
                if (resp.isSuccessful) {
                    campanhas.clear()
                    campanhas.addAll(resp.body() ?: emptyList())
                    binding.rvCampanhas.adapter?.notifyDataSetChanged()
                }
            } catch (_: Exception) {
                Toast.makeText(this@CampanhasActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarDelete(campanha: CampanhaResponse) {
        AlertDialog.Builder(this)
            .setTitle("Deletar campanha")
            .setMessage("Deletar \"${campanha.nome}\"?")
            .setPositiveButton("Deletar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        RetrofitClient.apiService.deleteCampanha(
                            SessionManager.getAuthHeader(), estudoRemoteId, campanha.id
                        )
                        carregarCampanhas()
                    } catch (_: Exception) { }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }
}

// ── Adapter inline ──────────────────────────────────────────────────────────

class CampanhaAdapter(
    private val items: List<CampanhaResponse>,
    private val onItemClick: (CampanhaResponse) -> Unit,
    private val onDeleteClick: (CampanhaResponse) -> Unit
) : RecyclerView.Adapter<CampanhaAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView    = view.findViewById(R.id.tvCampanhaNome)
        val tvData: TextView    = view.findViewById(R.id.tvCampanhaData)
        val ivDelete: ImageView = view.findViewById(R.id.ivDeleteCampanha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_campanha, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNome.text = item.nome
        holder.tvData.text = formatarData(item.createdAt)
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.ivDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size

    private fun formatarData(iso: String) = try {
        val p = iso.substring(0, 10).split("-"); "${p[2]}/${p[1]}/${p[0]}"
    } catch (_: Exception) { iso }
}

package com.kheprix.api

import com.kheprix.models.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ──────────────────────────────────────────────
    // AUTENTICAÇÃO
    // ──────────────────────────────────────────────

    @POST("autenticacao/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("autenticacao/solicitar_redefinicao")
    suspend fun solicitarRedefinicao(@Body body: EmailRequest): Response<MensagemResponse>

    @POST("autenticacao/validar_token_redefinicao")
    suspend fun validarTokenRedefinicao(@Body body: TokenRequest): Response<ValidoResponse>

    @POST("autenticacao/validar_token")
    suspend fun validarToken(@Body body: TokenRequest): Response<ValidoResponse>

    @POST("autenticacao/redefinir_senha")
    suspend fun redefinirSenha(@Body body: RedefinirSenhaRequest): Response<MensagemResponse>

    // ──────────────────────────────────────────────
    // USUÁRIOS
    // ──────────────────────────────────────────────

    @POST("usuarios/autocadastro")
    suspend fun autocadastro(@Body body: AutocadastroRequest): Response<UsuarioResponse>

    // ──────────────────────────────────────────────
    // DASHBOARD
    // ──────────────────────────────────────────────

    @GET("dashboard")
    suspend fun getDashboard(
        @Header("Authorization") token: String
    ): Response<DashboardResponse>

    // ──────────────────────────────────────────────
    // ESTUDOS
    // ──────────────────────────────────────────────

    @GET("estudos")
    suspend fun getEstudos(
        @Header("Authorization") token: String,
        @Query("nome") nome: String? = null,
        @Query("criado_a_partir_de") criadoApartirDe: String? = null,
        @Query("criado_ate") criadoAte: String? = null,
        @Query("atualizado_a_partir_de") atualizadoApartirDe: String? = null,
        @Query("atualizado_ate") atualizadoAte: String? = null
    ): Response<List<EstudoResponse>>

    @POST("estudos")
    suspend fun postEstudo(
        @Header("Authorization") token: String,
        @Body body: EstudoRequest
    ): Response<EstudoResponse>

    @DELETE("estudos/{id}")
    suspend fun deleteEstudo(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<ResponseBody>

    // ──────────────────────────────────────────────
    // VARIÁVEIS
    // ──────────────────────────────────────────────

    @GET("estudos/{estudo_id}/variaveis")
    suspend fun getVariaveis(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Query("nivel_aplicacao") nivelAplicacao: String? = null
    ): Response<List<VariavelResponse>>

    // ──────────────────────────────────────────────
    // ESPÉCIES
    // ──────────────────────────────────────────────

    @GET("estudos/{estudo_id}/especies")
    suspend fun getEspecies(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Query("nome_popular") nomePopular: String? = null
    ): Response<List<EspecieResponse>>

    @GET("estudos/{estudo_id}/especies/{id}")
    suspend fun getEspecie(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("id") id: Int
    ): Response<EspecieResponse>

    @POST("estudos/{estudo_id}/especies")
    suspend fun postEspecie(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Body body: EspecieRequest
    ): Response<EspecieResponse>

    @PATCH("estudos/{estudo_id}/especies/{id}")
    suspend fun patchEspecie(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("id") id: Int,
        @Body body: EspeciePatchRequest
    ): Response<EspecieResponse>

    @DELETE("estudos/{estudo_id}/especies/{id}")
    suspend fun deleteEspecie(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("id") id: Int
    ): Response<ResponseBody>

    // ──────────────────────────────────────────────
    // COLABORADORES
    // ──────────────────────────────────────────────

    @GET("estudos/{estudo_id}/colaboradores")
    suspend fun getColaboradores(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int
    ): Response<List<ColaboradorResponse>>

    @PATCH("estudos/{estudo_id}/colaboradores/{id}")
    suspend fun patchColaborador(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("id") id: Int,
        @Body body: ColaboradorPatchRequest
    ): Response<ColaboradorResponse>

    @DELETE("estudos/{estudo_id}/colaboradores/{id}")
    suspend fun deleteColaborador(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("id") id: Int
    ): Response<ResponseBody>

    // ──────────────────────────────────────────────
    // CONVITES (enviados)
    // ──────────────────────────────────────────────

    @POST("estudos/{estudo_id}/convites")
    suspend fun postConvite(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Body body: ConviteRequest
    ): Response<ConviteResponse>

    @GET("estudos/{estudo_id}/convites")
    suspend fun getConvites(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Query("status") status: String? = null
    ): Response<List<ConviteListResponse>>

    @DELETE("estudos/{estudo_id}/convites/{id}")
    suspend fun deleteConvite(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("id") id: Int
    ): Response<ResponseBody>

    // ──────────────────────────────────────────────
    // CONVITES (recebidos)
    // ──────────────────────────────────────────────

    @GET("convites")
    suspend fun getConvitesRecebidos(
        @Header("Authorization") token: String
    ): Response<List<ConviteRecebidoResponse>>

    @GET("convites/{token}")
    suspend fun getConvitePorToken(
        @Path("token") token: String
    ): Response<ConviteDetalheResponse>

    @POST("convites/{token}/aceitar")
    suspend fun aceitarConvite(
        @Header("Authorization") authToken: String,
        @Path("token") token: String
    ): Response<MensagemResponse>

    @POST("convites/{token}/recusar")
    suspend fun recusarConvite(
        @Header("Authorization") authToken: String,
        @Path("token") token: String
    ): Response<MensagemResponse>

    // ──────────────────────────────────────────────
    // CÓDIGO DE ACESSO
    // ──────────────────────────────────────────────

    @GET("estudos/{estudo_id}/codigo_acesso")
    suspend fun getCodigoAcesso(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int
    ): Response<CodigoAcessoResponse>

    @PATCH("estudos/{estudo_id}/codigo_acesso")
    suspend fun patchCodigoAcesso(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Body body: CodigoAcessoPatchRequest
    ): Response<CodigoAcessoResponse>

    // ──────────────────────────────────────────────
    // AUTOCADASTRO EM ESTUDO
    // ──────────────────────────────────────────────

    @POST("estudos/ingressar")
    suspend fun ingressarEstudo(
        @Header("Authorization") token: String,
        @Body body: IngressarRequest
    ): Response<IngressarResponse>

    // ──────────────────────────────────────────────
    // CAMPANHAS
    // ──────────────────────────────────────────────

    @GET("estudos/{estudo_id}/campanhas")
    suspend fun getCampanhas(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int
    ): Response<List<CampanhaResponse>>

    @GET("estudos/{estudo_id}/campanhas/{id}")
    suspend fun getCampanha(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("id") id: Int
    ): Response<CampanhaResponse>

    @POST("estudos/{estudo_id}/campanhas")
    suspend fun postCampanha(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Body body: CampanhaRequest
    ): Response<CampanhaResponse>

    @PATCH("estudos/{estudo_id}/campanhas/{id}")
    suspend fun patchCampanha(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("id") id: Int,
        @Body body: CampanhaRequest
    ): Response<CampanhaResponse>

    @DELETE("estudos/{estudo_id}/campanhas/{id}")
    suspend fun deleteCampanha(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("id") id: Int
    ): Response<ResponseBody>

    // ──────────────────────────────────────────────
    // UNIDADES AMOSTRAIS
    // ──────────────────────────────────────────────

    @GET("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais")
    suspend fun getUnidades(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int
    ): Response<List<UnidadeResponse>>

    @GET("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{id}")
    suspend fun getUnidade(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("id") id: Int
    ): Response<UnidadeResponse>

    @POST("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais")
    suspend fun postUnidade(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Body body: UnidadeRequest
    ): Response<UnidadeResponse>

    @PATCH("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{id}")
    suspend fun patchUnidade(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("id") id: Int,
        @Body body: UnidadeRequest
    ): Response<UnidadeResponse>

    @DELETE("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{id}")
    suspend fun deleteUnidade(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("id") id: Int
    ): Response<ResponseBody>

    // ──────────────────────────────────────────────
    // EVENTOS DE AMOSTRAGEM
    // ──────────────────────────────────────────────

    @GET("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{unidade_id}/eventos_amostragem")
    suspend fun getEventos(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("unidade_id") unidadeId: Int
    ): Response<List<EventoResponse>>

    @GET("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{unidade_id}/eventos_amostragem/{id}")
    suspend fun getEvento(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("unidade_id") unidadeId: Int,
        @Path("id") id: Int
    ): Response<EventoResponse>

    @POST("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{unidade_id}/eventos_amostragem")
    suspend fun postEvento(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("unidade_id") unidadeId: Int,
        @Body body: EventoRequest
    ): Response<EventoResponse>

    @PATCH("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{unidade_id}/eventos_amostragem/{id}")
    suspend fun patchEvento(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("unidade_id") unidadeId: Int,
        @Path("id") id: Int,
        @Body body: EventoRequest
    ): Response<EventoResponse>

    @DELETE("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{unidade_id}/eventos_amostragem/{id}")
    suspend fun deleteEvento(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("unidade_id") unidadeId: Int,
        @Path("id") id: Int
    ): Response<ResponseBody>

    // ──────────────────────────────────────────────
    // REGISTROS DE OCORRÊNCIA
    // ──────────────────────────────────────────────

    @GET("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{unidade_id}/eventos_amostragem/{evento_id}/registro_ocorrencias")
    suspend fun getRegistros(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("unidade_id") unidadeId: Int,
        @Path("evento_id") eventoId: Int
    ): Response<List<RegistroResponse>>

    @GET("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{unidade_id}/eventos_amostragem/{evento_id}/registro_ocorrencias/{id}")
    suspend fun getRegistro(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("unidade_id") unidadeId: Int,
        @Path("evento_id") eventoId: Int,
        @Path("id") id: Int
    ): Response<RegistroResponse>

    @POST("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{unidade_id}/eventos_amostragem/{evento_id}/registro_ocorrencias")
    suspend fun postRegistro(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("unidade_id") unidadeId: Int,
        @Path("evento_id") eventoId: Int,
        @Body body: RegistroRequest
    ): Response<RegistroResponse>

    @PATCH("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{unidade_id}/eventos_amostragem/{evento_id}/registro_ocorrencias/{id}")
    suspend fun patchRegistro(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("unidade_id") unidadeId: Int,
        @Path("evento_id") eventoId: Int,
        @Path("id") id: Int,
        @Body body: RegistroPatchRequest
    ): Response<RegistroResponse>

    @DELETE("estudos/{estudo_id}/campanhas/{campanha_id}/unidades_amostrais/{unidade_id}/eventos_amostragem/{evento_id}/registro_ocorrencias/{id}")
    suspend fun deleteRegistro(
        @Header("Authorization") token: String,
        @Path("estudo_id") estudoId: Int,
        @Path("campanha_id") campanhaId: Int,
        @Path("unidade_id") unidadeId: Int,
        @Path("evento_id") eventoId: Int,
        @Path("id") id: Int
    ): Response<ResponseBody>
}

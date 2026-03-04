
class ProjetosController < ApplicationController
  before_action :autenticar_requisicao!
  before_action :carregar_projeto, only: %i[show update destroy]

  # GET /projetos
  def index
    projetos = servico.listar
    render json: projetos, status: :ok
  end

  # GET /projetos/:id
  def show
    render json: @projeto, status: :ok
  end

  # POST /projetos
  def create
    dto = CriarProjetoDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    projeto = servico.criar(nome: dto.nome, descricao: dto.descricao)

    if projeto.persisted?
      render json: projeto, status: :created
    else
      render json: { erros: projeto.errors.full_messages }, status: :unprocessable_entity
    end
  end

  # PATCH /projetos/:id
  def update
    dto = AtualizarProjetoDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    projeto = servico.atualizar(
      projeto: @projeto,
      nome: dto.nome,
      descricao: dto.descricao,
      ativo: dto.ativo,
    )

    if projeto.errors.empty?
      render json: projeto, status: :ok
    else
      render json: { erros: projeto.errors.full_messages }, status: :unprocessable_entity
    end
  end

  # DELETE /projetos/:id
  def destroy
    servico.destruir(@projeto)
    head :no_content
  end

  private

  # Memoiza a instância do service
  def servico
    @servico ||= ServicoProjeto.new
  end

  # before_action — carrega o projeto ou retorna 404
  def carregar_projeto
    @projeto = servico.buscar_por_id(params[:id])

    unless @projeto
      render json: { erro: "Projeto não encontrado" }, status: :not_found
    end
  end
end
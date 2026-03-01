# frozen_string_literal: true

class UsuariosController < ApplicationController
  def autocadastro
    dto = AutocadastroDto.new(params)

    unless dto.valid?
      render json: { erros: dto.errors.full_messages }, status: :unprocessable_entity
      return
    end

    usuario = ServicoUsuario.new.cadastrar(
      nome: dto.nome,
      email: dto.email,
      senha: dto.senha,
    )

    if usuario.persisted?
      render json: usuario, status: :created
    else
      render json: { erros: usuario.errors.full_messages }, status: :unprocessable_entity
    end
  end
end

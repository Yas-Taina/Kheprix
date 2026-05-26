ENV["RAILS_ENV"] ||= "test"
require_relative "../config/environment"
require "rails/test_help"

require "rails/test_unit/line_filtering"
if Minitest::VERSION.to_i >= 6
  module Rails
    module LineFiltering
      def run_suite(reporter, options = {})
        options = options.merge(filter: Rails::TestUnit::Runner.compose_filter(self, options[:filter]))
        super
      end

      def run(*args)
        super
      end
    end
  end
end

module ActiveSupport
  class TestCase
    parallelize(workers: 1)
    fixtures :all
  end
end

class PermissoesBase < ActionDispatch::IntegrationTest
  setup do
    @proprietario    = Usuario.create!(nome: "Proprietario",   email: "prop_#{SecureRandom.hex(4)}@teste.com",  password: "senha123")
    @colaborador_user = Usuario.create!(nome: "Colaborador",   email: "colab_#{SecureRandom.hex(4)}@teste.com", password: "senha123")
    @forasteiro      = Usuario.create!(nome: "Forasteiro",     email: "fora_#{SecureRandom.hex(4)}@teste.com",  password: "senha123")

    @estudo = Estudo.create!(nome: "Estudo Teste")
    Colaborador.create!(estudo_id: @estudo.id, usuario_id: @proprietario.id,    perfil: :proprietario)
    Colaborador.create!(estudo_id: @estudo.id, usuario_id: @colaborador_user.id, perfil: :colaborador)

    @token_prop      = gerar_token(@proprietario)
    @token_colab     = gerar_token(@colaborador_user)
    @token_forasteiro = gerar_token(@forasteiro)
  end

  private

  def gerar_token(usuario)
    ServicoAutenticacao.new.send(:gerar_token, usuario)
  end

  def auth(token)
    { "Authorization" => "Bearer #{token}" }
  end
end

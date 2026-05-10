Rails.application.routes.draw do
  # Define your application routes per the DSL in https://guides.rubyonrails.org/routing.html

  # Reveal health status on /up that returns 200 if the app boots with no exceptions, otherwise 500.
  # Can be used by load balancers and uptime monitors to verify that the app is live.
  get "up" => "rails/health#show", as: :rails_health_check

  post "autenticacao/login", to: "autenticacao#login"
  post "autenticacao/solicitar_redefinicao", to: "autenticacao#solicitar_redefinicao"
  post "autenticacao/redefinir_senha", to: "autenticacao#redefinir_senha"
  post "autenticacao/validar_token_redefinicao", to: "autenticacao#validar_token_redefinicao"
  post "autenticacao/validar_token", to: "autenticacao#validar_token"

  post "usuarios/autocadastro", to: "usuarios#autocadastro"

  post "estudos/ingressar", to: "autocadastro_estudo#create"

  get "dashboard", to: "dashboard#index"

  resources :estudos do
    get "dashboard", on: :member, to: "dashboard_estudo#show"
    get "exportar_dados", on: :member, to: "exportar_dados#exportar_dados"
    resources :analises, only: [] do
      collection do
        post :executar
      end
    end
    resources :campanhas do
      resources :unidades_amostrais, only: %i[index show create update destroy] do
        resources :eventos_amostragem, only: %i[index show create update destroy] do
          resources :registro_ocorrencias, only: %i[index show create update destroy]
        end
      end
    end
    resources :variaveis, only: %i[index]
    resources :especies, only: %i[index show create update destroy]
    resources :colaboradores, only: %i[index update destroy]
    resources :convites, only: %i[index create destroy]
    resource :codigo_acesso, only: %i[show update], controller: "codigo_acesso"
  end

  get "convites", to: "gerenciar_convites#index", as: :convites_recebidos
  get "convites/:token", to: "gerenciar_convites#show", as: :convite_por_token
  post "convites/:token/aceitar", to: "gerenciar_convites#aceitar", as: :aceitar_convite
  post "convites/:token/recusar", to: "gerenciar_convites#recusar", as: :recusar_convite

  get "fotos/estudos/:estudo_id/:tipo/:arquivo", to: "fotos#show", as: :foto, constraints: { arquivo: /[^\/]+/ }

  get "analises/estudos/:estudo_id/:chave/:arquivo",
      to: "resultados_analise#show",
      as: :resultado_analise,
      constraints: { arquivo: /[^\/]+/ }

  post "chatbot/query",    to: "chatbot#query"
  post "chatbot/insights", to: "chatbot#insights"
end

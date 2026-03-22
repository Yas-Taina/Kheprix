Rails.application.routes.draw do
  # Define your application routes per the DSL in https://guides.rubyonrails.org/routing.html

  # Reveal health status on /up that returns 200 if the app boots with no exceptions, otherwise 500.
  # Can be used by load balancers and uptime monitors to verify that the app is live.
  get "up" => "rails/health#show", as: :rails_health_check

  post "autenticacao/login", to: "autenticacao#login"
  post "autenticacao/solicitar_redefinicao", to: "autenticacao#solicitar_redefinicao"
  post "autenticacao/redefinir_senha", to: "autenticacao#redefinir_senha"
  post "autenticacao/validar_token_redefinicao", to: "autenticacao#validar_token_redefinicao"

  post "usuarios/autocadastro", to: "usuarios#autocadastro"

  post "estudos/ingressar", to: "autocadastro_estudo#create"

  resources :estudos do
    resources :campanhas do
      resources :unidades_amostrais, only: %i[index show create update destroy]
    end
    resources :campanhas
    resources :especies, only: %i[index show create update destroy]
    resources :colaboradores, only: %i[index update destroy]
    resources :convites, only: %i[index create destroy]
    resource :codigo_acesso, only: %i[show update destroy], controller: "codigo_acesso"
  end

  get "convites", to: "gerenciar_convites#index", as: :convites_recebidos
  get "convites/:token", to: "gerenciar_convites#show", as: :convite_por_token
  post "convites/:token/aceitar", to: "gerenciar_convites#aceitar", as: :aceitar_convite
  post "convites/:token/recusar", to: "gerenciar_convites#recusar", as: :recusar_convite
end

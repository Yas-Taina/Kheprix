# frozen_string_literal: true

class ApplicationMailer < ActionMailer::Base
  default from: -> { ENV.fetch("EMAIL_REMETENTE", "noreply@kheprix.com") }
  layout "mailer"
end

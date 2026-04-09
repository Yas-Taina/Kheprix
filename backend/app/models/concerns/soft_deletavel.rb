# frozen_string_literal: true

module SoftDeletavel
  extend ActiveSupport::Concern

  included do
    default_scope { where(deleted_at: nil) }
    scope :deletados, -> { unscope(where: :deleted_at).where.not(deleted_at: nil) }
    scope :com_deletados, -> { unscope(where: :deleted_at) }
  end

  def soft_delete
    update_columns(deleted_at: Time.zone.now)
  end

  def restaurar
    update_columns(deleted_at: nil)
  end

  def deletado?
    deleted_at.present?
  end
end

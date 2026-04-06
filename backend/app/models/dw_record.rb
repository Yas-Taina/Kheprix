# frozen_string_literal: true

class DwRecord < ActiveRecord::Base
  self.abstract_class = true

  connects_to database: { writing: :dw, reading: :dw }
end

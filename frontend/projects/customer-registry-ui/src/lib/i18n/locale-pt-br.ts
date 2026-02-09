/**
 * Brazilian Portuguese locale translations (default locale).
 *
 * Fallback behavior: if a key is not found in the active locale,
 * the i18n service falls back through: host overrides -> built-in[locale]
 * -> built-in['en'] -> built-in['pt-BR'] -> raw key.
 * All locale files should contain the same set of keys to avoid
 * unexpected fallback behavior.
 */
export const LOCALE_PT_BR: Record<string, string> = {
  // General
  'label.customer': 'Cliente',
  'label.customers': 'Clientes',
  'label.create': 'Criar',
  'label.save': 'Salvar',
  'label.cancel': 'Cancelar',
  'label.delete': 'Excluir',
  'label.edit': 'Editar',
  'label.search': 'Buscar',
  'label.loading': 'Carregando...',
  'label.clear': 'Limpar',
  'label.all': 'Todos',
  'label.confirm': 'Confirmar',
  'label.back': 'Voltar',
  'label.add': 'Adicionar',
  'label.remove': 'Remover',
  'label.noResults': 'Nenhum resultado encontrado.',
  'label.actions': 'Ações',

  // Customer types
  'customer.type.PF': 'Pessoa Física',
  'customer.type.PJ': 'Pessoa Jurídica',

  // Customer status
  'customer.status.DRAFT': 'Rascunho',
  'customer.status.ACTIVE': 'Ativo',
  'customer.status.SUSPENDED': 'Suspenso',
  'customer.status.CLOSED': 'Encerrado',

  // Customer fields
  'field.type': 'Tipo',
  'field.document': 'Documento',
  'field.displayName': 'Nome de Exibição',
  'field.status': 'Situação',
  'field.createdAt': 'Criado em',
  'field.updatedAt': 'Atualizado em',

  // Address
  'field.address': 'Endereço',
  'field.addresses': 'Endereços',
  'field.street': 'Logradouro',
  'field.number': 'Número',
  'field.complement': 'Complemento',
  'field.neighborhood': 'Bairro',
  'field.city': 'Cidade',
  'field.state': 'Estado',
  'field.zipCode': 'CEP',
  'field.country': 'País',

  // Contact
  'field.contact': 'Contato',
  'field.contacts': 'Contatos',
  'field.contactType': 'Tipo de Contato',
  'field.contactValue': 'Valor',
  'field.contactPrimary': 'Principal',
  'contact.type.EMAIL': 'E-mail',
  'contact.type.PHONE': 'Telefone',
  'contact.type.MOBILE': 'Celular',

  // Validation
  'validation.required': 'Campo obrigatório.',
  'validation.cpf.invalid': 'CPF inválido.',
  'validation.cnpj.invalid': 'CNPJ inválido.',
  'validation.document.invalid': 'Documento inválido.',
  'validation.minLength': 'Mínimo de {0} caracteres.',
  'validation.maxLength': 'Máximo de {0} caracteres.',

  // Messages
  'message.createSuccess': 'Cliente criado com sucesso.',
  'message.updateSuccess': 'Cliente atualizado com sucesso.',
  'message.deleteSuccess': 'Cliente excluído com sucesso.',
  'message.error': 'Ocorreu um erro. Tente novamente.',
};

import type { ErrorCode } from './errors';

// The API's own messages are English and never reach the user: every code has its Spanish text here.
export const errorMessages: Record<ErrorCode, string> = {
	CLIENT_NOT_FOUND: 'No existe ese cliente. Puede que se haya borrado.',
	CLIENT_DUPLICATE_CODE: 'Ya existe un cliente con ese código.',
	CLIENT_EMAIL_NOT_CONFIGURED:
		'Este cliente no tiene correo electrónico. Añádelo en su ficha para poder enviarle documentos.',
	CLIENT_FISCAL_DATA_MISSING:
		'Al cliente le faltan datos para emitir la factura: NIF, dirección, código postal o población. Complétalos en su ficha.',
	ARTICLE_NOT_FOUND: 'No existe ese artículo. Puede que se haya borrado.',
	ARTICLE_DUPLICATE_CODE: 'Ya existe un artículo con ese código.',
	DELIVERY_NOTE_NOT_FOUND: 'No existe ese albarán. Puede que se haya borrado.',
	DELIVERY_NOTE_DUPLICATE_CODE: 'Ya existe un albarán con ese número.',
	DELIVERY_NOTE_ALREADY_INVOICED:
		'Este albarán está en una factura emitida y ya no se puede cambiar ni borrar.',
	INVOICE_NOT_FOUND: 'No existe esa factura. Puede que se haya borrado.',
	INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED: 'Uno de los albaranes ya está en otra factura.',
	INVOICE_MIXED_CLIENTS: 'Todos los albaranes de una factura tienen que ser del mismo cliente.',
	INVOICE_MIXED_TAX_RATES:
		'Los albaranes llevan tipos de IVA distintos. Una factura solo puede llevar albaranes con el mismo IVA.',
	INVOICE_MIXED_MONTHS:
		'Los albaranes son de meses distintos. Una factura solo puede llevar albaranes del mismo mes.',
	INVOICE_ALREADY_ISSUED: 'Esta factura ya está emitida y no se puede cambiar.',
	INVOICE_NOT_ISSUED: 'Esta factura todavía es un borrador. Emítela primero.',
	TAX_CONFIG_NOT_FOUND: 'No hay ningún IVA configurado para esa fecha. Añádelo en Ajustes.',
	TAX_CONFIG_ALREADY_EXISTS: 'Ya hay un IVA que empieza en esa fecha.',
	USER_PROFILE_NOT_FOUND: 'Faltan tus datos de empresa. Rellénalos en Ajustes.',
	USER_PROFILE_ALREADY_EXISTS: 'Tus datos de empresa ya están guardados.',
	USER_PROFILE_LOGO_NOT_FOUND: 'Todavía no has subido ningún logotipo.',
	USER_PROFILE_INVALID_LOGO_FORMAT: 'El logotipo tiene que ser una imagen JPG o PNG.',
	MAIL_NOT_CONFIGURED: 'El correo no está configurado. Rellena sus datos en Ajustes.',
	EMAIL_SEND_FAILED:
		'No se ha podido enviar el correo. Comprueba la conexión a internet y los datos del correo en Ajustes.',
	VALIDATION_ERROR: 'Hay datos que revisar.',
	INTERNAL_ERROR: 'Ha ocurrido un error inesperado. La operación no se ha completado.'
};

export function isErrorCode(value: unknown): value is ErrorCode {
	return typeof value === 'string' && Object.hasOwn(errorMessages, value);
}

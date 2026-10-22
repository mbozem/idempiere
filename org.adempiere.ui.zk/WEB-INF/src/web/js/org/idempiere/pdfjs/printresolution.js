/*
 * Applies the configured print raster resolution to the embedded pdf.js viewer.
 *
 * pdf.js prints what it has rendered into a canvas, so a printout can not be
 * sharper than the raster of that canvas. Its printResolution option is 150 dpi
 * by default, which a 300 dpi printer can not sharpen again, and pdf.js only
 * takes it from the viewer options - not from the document URL.
 *
 * iDempiere passes the value of the ZK_PDF_JS_PRINT_RESOLUTION system
 * configuration as the printresolution hash parameter (see AEnv.toPdfJsUrl) and
 * this module turns it into the viewer option. It is loaded by
 * web/js/pdf.js/web/viewer.html after viewer.mjs, so the option is set long
 * before the first print request reads it.
 */
(() => {
	//	the bounds and the default of AEnv, kept in sync on purpose
	const DEFAULT_PRINT_RESOLUTION = 150;
	const MIN_PRINT_RESOLUTION = 72;
	const MAX_PRINT_RESOLUTION = 600;

	//	pdf.js lower cases the names of the hash parameters
	const parameter = new URLSearchParams(document.location.hash.substring(1)).get('printresolution');
	const requested = parseInt(parameter, 10);
	if (!Number.isInteger(requested))
		return;

	const printResolution = Math.min(Math.max(requested, MIN_PRINT_RESOLUTION), MAX_PRINT_RESOLUTION);
	if (printResolution === DEFAULT_PRINT_RESOLUTION)
		return;

	if (typeof window.PDFViewerApplicationOptions === 'undefined') {
		console.error('idempiere/pdfjs/printresolution.js: the pdf.js viewer options are not available,'
			+ ' the print resolution of ' + printResolution + ' dpi is ignored.');
		return;
	}

	window.PDFViewerApplicationOptions.set('printResolution', printResolution);
})();

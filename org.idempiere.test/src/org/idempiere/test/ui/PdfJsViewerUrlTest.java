/**********************************************************************
 * This file is part of iDempiere ERP Open Source                     *
 * http://www.idempiere.org                                           *
 *                                                                    *
 * Copyright (C) Contributors                                         *
 *                                                                    *
 * This program is free software; you can redistribute it and/or      *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2     *
 * of the License, or (at your option) any later version.              *
 **********************************************************************/
package org.idempiere.test.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.adempiere.webui.apps.AEnv;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the print raster resolution of the embedded pdf.js viewer, see
 * {@link AEnv#toPdfJsUrl(String, String, int)}.
 */
public class PdfJsViewerUrlTest {

	private static final String VIEWER = "/zkau/web/js/pdf.js/web/viewer.html?file=";

	private static final String PDF = "/zkau/media/56/report.pdf";

	@Test
	public void testPdfJsUrl() {
		assertEquals(VIEWER + PDF, AEnv.toPdfJsUrl(VIEWER, PDF, AEnv.PDF_JS_DEFAULT_PRINT_RESOLUTION));
	}

	@Test
	public void testPdfJsUrlWithPrintResolution() {
		assertEquals(VIEWER + PDF + "#printresolution=300", AEnv.toPdfJsUrl(VIEWER, PDF, 300));
	}

	@Test
	public void testPdfJsUrlWithoutPrintResolution() {
		//	an unknown resolution must not change the url of the viewer
		assertEquals(VIEWER + PDF, AEnv.toPdfJsUrl(VIEWER, PDF, -1));
	}

}

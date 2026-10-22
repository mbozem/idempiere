-- IDEMPIERE-7168 Configurable print raster resolution for the embedded pdf.js viewer
-- Dictionary IDs reserved at developer.idempiere.com for IDEMPIERE-7168
SELECT register_migration_script('202610221300_IDEMPIERE-7168.sql') FROM dual;

-- Oct 22, 2026, 1:00:37 PM
INSERT INTO AD_SysConfig (AD_SysConfig_ID,AD_Client_ID,AD_Org_ID,Created,Updated,CreatedBy,UpdatedBy,IsActive,Name,Value,Description,EntityType,ConfigurationLevel,AD_SysConfig_UU) VALUES (200336,0,0,TO_TIMESTAMP('2026-10-22 13:00:36','YYYY-MM-DD HH24:MI:SS'),TO_TIMESTAMP('2026-10-22 13:00:36','YYYY-MM-DD HH24:MI:SS'),100,100,'Y','ZK_PDF_JS_PRINT_RESOLUTION','150','Print raster resolution in dpi of the embedded pdf.js viewer. pdf.js prints bitmaps, so a printout is never sharper than this value. Default 150, 300 recommended for printing, 72 to 600 accepted.','D','C','8d8447b3-8c15-43ce-ab10-561d9c5752e5')
;

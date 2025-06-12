(async function () {
  var cardFiles = [
    "nido_card_b_1.jpg", "nido_card_b_2.jpg", "nido_card_b_3.jpg", "nido_card_b_4.jpg", "nido_card_b_5.jpg",
    "nido_card_b_6.jpg", "nido_card_b_7.jpg", "nido_card_b_8.jpg", "nido_card_b_9.jpg",

    "nido_card_g_1.jpg", "nido_card_g_2.jpg", "nido_card_g_3.jpg", "nido_card_g_4.jpg", "nido_card_g_5.jpg",
    "nido_card_g_6.jpg", "nido_card_g_7.jpg", "nido_card_g_8.jpg", "nido_card_g_9.jpg",

    "nido_card_m_1.jpg", "nido_card_m_2.jpg", "nido_card_m_3.jpg", "nido_card_m_4.jpg", "nido_card_m_5.jpg",
    "nido_card_m_6.jpg", "nido_card_m_7.jpg", "nido_card_m_8.jpg", "nido_card_m_9.jpg",

    "nido_card_o_1.jpg", "nido_card_o_2.jpg", "nido_card_o_3.jpg", "nido_card_o_4.jpg", "nido_card_o_5.jpg",
    "nido_card_o_6.jpg", "nido_card_o_7.jpg", "nido_card_o_8.jpg", "nido_card_o_9.jpg",

    "nido_card_p_1.jpg", "nido_card_p_2.jpg", "nido_card_p_3.jpg", "nido_card_p_4.jpg", "nido_card_p_5.jpg",
    "nido_card_p_6.jpg", "nido_card_p_7.jpg", "nido_card_p_8.jpg", "nido_card_p_9.jpg",

    "nido_card_r_1.jpg", "nido_card_r_2.jpg", "nido_card_r_3.jpg", "nido_card_r_4.jpg", "nido_card_r_5.jpg",
    "nido_card_r_6.jpg", "nido_card_r_7.jpg", "nido_card_r_8.jpg", "nido_card_r_9.jpg"
  ];

  for (let i = 0; i < cardFiles.length; i++) {
    const name = cardFiles[i];
    console.log(`▶️ Processing ${name} (${i + 1} / ${cardFiles.length})`);

    // Locate card document
    const cardDoc = app.documents.find(d => d.name === name);
    if (!cardDoc) {
      console.warn(`⚠️ Skipping: ${name} not found.`);
      continue;
    }

    // Locate template
    const templateDoc = app.documents.find(d => d.name === "card_template.psd");
    if (!templateDoc) {
      alert("❌ 'card_template.psd' is not open — please open it first.");
      return;
    }

    // Copy card
    cardDoc.activate();
    cardDoc.activeLayer.copy();

    // Paste into template
    templateDoc.activate();
    templateDoc.paste();

    try {
      await app.activeDocument.loadSelection("cut_mask");
      await app.runMenuCommand("inverse");
      await app.runMenuCommand("delete");
      await app.runMenuCommand("deselect");
    } catch (e) {
      alert("❌ Could not apply cut_mask. Did you forget to save the selection?");
      return;
    }

    // Export
    const exportName = name.replace(".jpg", "_cut.png");
    await app.saveAsPNG(exportName);
    console.log(`✅ Saved as ${exportName}`);

    // Clean up
    app.activeDocument.activeLayer.remove(); // remove pasted layer
  }

  alert("🎉 All cards processed and exported!");
})();

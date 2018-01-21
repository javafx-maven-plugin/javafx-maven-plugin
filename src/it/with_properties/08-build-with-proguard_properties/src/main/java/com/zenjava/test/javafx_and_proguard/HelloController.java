package com.zenjava.test.javafx_and_proguard;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloController{
    @FXML private Label lblClassName;

    @FXML
    private void initialize() {
        lblClassName.setText(this.getClass().getCanonicalName());
    }

}

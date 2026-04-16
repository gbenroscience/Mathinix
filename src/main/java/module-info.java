module com.github.gbenroscience.mathinix {
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.base; 
    requires parser.ng;
    opens com.github.gbenroscience.mathinix to javafx.graphics, javafx.fxml;
    exports com.github.gbenroscience.mathinix;
}

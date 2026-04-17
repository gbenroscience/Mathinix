module com.github.gbenroscience.mathinix {
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.base; 
    requires parser.ng;
    requires java.desktop;
    // Use these specific automatic module names
    requires jzy3d.api; 
    requires jzy3d.javafx;
    opens com.github.gbenroscience.mathinix to javafx.graphics, javafx.fxml;
    exports com.github.gbenroscience.mathinix;
}

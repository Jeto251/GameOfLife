module com.jeto.game {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.jeto.game to javafx.fxml;
    exports com.jeto.game;
    exports com.jeto.game.points;
    opens com.jeto.game.points to javafx.fxml;
}
package sample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    ListView<FileInfo> filesList;

    Path root;

    Path selectedCopyFile;
    Path selectedMoveFile;
    Path selectedRenameFile;
    Path selectedFeaturesFile;

    @FXML
    private Label timeLabel;

    @FXML
    TextField pathField;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        freeSpaceMemory();
        Path root = Paths.get("1");
        filesList.getItems().addAll(scanFiles(root));

        filesList.setCellFactory(new Callback<>() {
            @Override
            public ListCell<FileInfo> call(ListView<FileInfo> fileInfoListView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(FileInfo item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("");
                        } else {
                            String formattedFilename = String.format("%s", item.getFilename());
                            String formattedFileLength = String.format("\t%d bytes", item.getLength());
                            if (item.getLength() == -1L) {
                                formattedFileLength = String.format("%s", "\t[DIR]");
                            }
                            if (item.getLength() == -2L) {
                                formattedFileLength = "";
                            }
                            //вывод имени и размера файла
                            String text = String.format("%-40s %s", formattedFilename, formattedFileLength);
                            setText(text);
                        }
                    }
                };
            }
        });
        goToPath(Paths.get("1"));
    }

    //закрытие программы
    public void menuItemFileExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    //переход в каталог
    public void goToPath(Path path) {
        root = path;
        pathField.setText(root.toAbsolutePath().toString());
        //отчистка коллекции
        filesList.getItems().clear();
        filesList.getItems().add(new FileInfo(FileInfo.UP_TOKEN, -2L));
        //заполнение коллекции
        filesList.getItems().addAll(scanFiles(path));
        //сортировка по размеру
        filesList.getItems().sort((o1, o2) -> (int) (o1.getLength() - o2.getLength()));
    }

    public List<FileInfo> scanFiles(Path root) {
        try {
            List<FileInfo> out = new ArrayList<>();
            List<Path> pathsInRoot = Files.list(root).collect(Collectors.toList());
            for (Path p : pathsInRoot) {
                out.add(new FileInfo(p));
            }
            return out;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сканирования файла: " + root);
        }

    }

    //Метод для определения свободного места(Обновляется каждые 30 секунд)
    public void freeSpaceMemory() {
        int oneGB = (int) Math.pow(2, 30);
        File file = new File("c:");
        long totalSpace = file.getTotalSpace() / oneGB;
        long freeSpace = file.getFreeSpace() / oneGB;
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(() -> {
            Platform.runLater(() -> timeLabel.setText("На вашем диске свободно " + freeSpace + "гб из " + totalSpace + "гб"));
            //Platform.runLater(() -> timeLabel.setText(" " + Math.random()));
        }, 0, 30, TimeUnit.SECONDS);

    }

    public void filesListClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            FileInfo fileInfo = filesList.getSelectionModel().getSelectedItem();
            if (fileInfo != null) {
                if (fileInfo.isDirectory()) {
                    Path pathTo = root.resolve(fileInfo.getFilename());
                    goToPath(pathTo);
                }
                if (fileInfo.isUpElement()) {
                    //getParent - ссылка на папку в которой вы находитесь
                    Path pathTo = root.toAbsolutePath().getParent();
                    goToPath(pathTo);
                }
            }
        }
    }

    public void refresh() {
        goToPath(root);
    }

    public void copyAction(ActionEvent actionEvent) {
        FileInfo fileInfo = filesList.getSelectionModel().getSelectedItem();
        if (selectedCopyFile == null && ( fileInfo == null || fileInfo.isDirectory() || fileInfo.isUpElement())) {
            return;
        }
        if (selectedCopyFile == null) {
            selectedCopyFile = root.resolve(fileInfo.getFilename());
            ((Button) actionEvent.getSource()).setText("Вставить " + fileInfo.getFilename());
            return;
        }
        if (selectedCopyFile != null) {
            try {
                Files.copy(selectedCopyFile, root.resolve(selectedCopyFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                selectedCopyFile = null;
                ((Button) actionEvent.getSource()).setText("Скопировать");
                refresh();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно скопировать файл");
                alert.showAndWait();
            }
        }
    }

    public void moveAction(ActionEvent actionEvent) {
        FileInfo fileInfo = filesList.getSelectionModel().getSelectedItem();
        if (selectedMoveFile == null && ( fileInfo == null || fileInfo.isDirectory() || fileInfo.isUpElement())) {
            return;
        }
        if (selectedMoveFile == null) {
            selectedMoveFile = root.resolve(fileInfo.getFilename());
            ((Button) actionEvent.getSource()).setText("Переместить " + fileInfo.getFilename());
            return;
        }
        if (selectedMoveFile != null) {
            try {
                Files.move(selectedMoveFile, root.resolve(selectedMoveFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                selectedMoveFile = null;
                ((Button) actionEvent.getSource()).setText("Переместить");
                refresh();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно переместить файл");
                alert.showAndWait();
            }
        }
    }

    public void menuItemDeleteAction(ActionEvent actionEvent) {
        FileInfo fileInfo = filesList.getSelectionModel().getSelectedItem();
        if (fileInfo == null || fileInfo.isDirectory() || fileInfo.isUpElement()) {
            return;
        }
        try {
            Files.delete(root.resolve(fileInfo.getFilename()));
            refresh();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно удалить файл");
            alert.showAndWait();
        }
        }

    public void menuItemFileRenameAction(ActionEvent actionEvent) {
        FileInfo fileInfo = filesList.getSelectionModel().getSelectedItem();
        selectedRenameFile = root.resolve(fileInfo.getFilename());

        Stage renameWindow = new Stage();
        TextArea renameArea = new TextArea(fileInfo.getFilename());
        Button btn1 = new Button("Переименовать");
        Button btn2 = new Button("Отмена");

        //При открытии окна, блокируются остальные окна, пока не закроется это
        renameWindow.initModality(Modality.APPLICATION_MODAL);

        renameArea.setPrefColumnCount(15);
        renameArea.setPrefRowCount(1);
        btn1.setOnAction(event -> {
            try {
                Files.move(selectedRenameFile, selectedRenameFile.resolveSibling(renameArea.getText()), StandardCopyOption.REPLACE_EXISTING);
                refresh();
                renameWindow.close();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно переименовать файл");
                alert.showAndWait();
            }
        });

        btn2.setOnAction(event -> renameWindow.close());
        FlowPane root = new FlowPane(Orientation.HORIZONTAL, 5, 5, renameArea, btn1, btn2);
        root.setAlignment(Pos.CENTER);
        Scene scene = new Scene(root, 350, 100);

        renameWindow.setScene(scene);
        renameWindow.setTitle("Переименовать файл");
        renameWindow.show();
    }

    public void menuItemFeaturesAction(ActionEvent actionEvent) {
        try {
            FileInfo fileInfo = filesList.getSelectionModel().getSelectedItem();
            selectedFeaturesFile = root.resolve(fileInfo.getFilename());

            Stage featuresWindow = new Stage();
            Label lbl1 = new Label();
            Label lbl2 = new Label();
            Label lbl3 = new Label();
            Label lbl4 = new Label();
            Label lbl5 = new Label();
            Label lbl6 = new Label();
            Button btn1 = new Button("Выйти");

            String probeContentType = Files.probeContentType(selectedFeaturesFile);
            lbl2.setText("Тип файла: " + probeContentType);





            BasicFileAttributes attr = Files.readAttributes(selectedFeaturesFile, BasicFileAttributes.class);
            lbl1.setText("Расположение: " + root.toAbsolutePath());
            lbl3.setText("Создан: " + attr.creationTime().toString());
            lbl4.setText("Открыт: " + attr.lastAccessTime());
            lbl5.setText("Изменён: " + attr.lastModifiedTime());
            lbl6.setText("Размер: " + attr.size() + "байт");



            btn1.setOnAction(event -> featuresWindow.close());
            FlowPane root = new FlowPane(Orientation.VERTICAL, 5, 5, lbl1, lbl2, lbl3, lbl4, lbl5, lbl6, btn1);
            root.setAlignment(Pos.CENTER);
            Scene scene = new Scene(root, 390, 200);

            featuresWindow.setScene(scene);
            featuresWindow.setTitle("Свойства файла " + selectedFeaturesFile.getFileName());
            featuresWindow.show();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно переименовать файл");
            alert.showAndWait();
        }
    }
}

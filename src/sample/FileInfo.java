package sample;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInfo {
    public static final String UP_TOKEN = "[..]";
    private String filename;
    private long length;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public FileInfo(Path path) {
            try {
                this.filename = path.getFileName().toString();
                //если путь указывает на директорию то тогда длина файла будет -1
                if(Files.isDirectory(path)) {
                    this.length = -1L;
                } else {
                    //если это файл, то запрашиваем длину у самого файла
                    this.length = Files.size(path);
                }
            } catch (IOException e) {
                throw new RuntimeException("Что-то не так с файлом: " + path.toAbsolutePath().toString());
            }
    }

    public boolean isDirectory() {
        return length == -1L;
    }

    public boolean isUpElement() {
        return length == -2L;
    }

    public FileInfo(String filename, long length) {
        this.filename = filename;
        this.length = length;
    }

}

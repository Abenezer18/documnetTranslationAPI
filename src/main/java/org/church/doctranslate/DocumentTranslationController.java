package org.church.doctranslate;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import okhttp3.ResponseBody;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


@RestController
public class DocumentTranslationController {

    private final String connectionString = "BlobEndpoint=https://noideawhatthisis.blob.core.windows.net/;QueueEndpoint=https://noideawhatthisis.queue.core.windows.net/;FileEndpoint=https://noideawhatthisis.file.core.windows.net/;TableEndpoint=https://noideawhatthisis.table.core.windows.net/;SharedAccessSignature=sv=2022-11-02&ss=bfqt&srt=sco&sp=rwdlacupiyx&se=2023-09-29T16:16:06Z&st=2023-09-01T08:16:06Z&spr=https,http&sig=8r60uAnRUPt%2F%2FvNyzkyaO2VuDZCyhNaleyWMrEabFmo%3D";
    private final String containerName = "translated";
    private final String tobetranslated = "tobetranslated";
    private final String translated = "translated";
    @PostMapping("/documenttranslate/{language}")
    public byte[] translate(@PathVariable String language, @RequestParam() MultipartFile file) throws IOException, InterruptedException {

        // upload file

        System.out.println("uploading file - " + file.getOriginalFilename());
        this.storeFile(file.getOriginalFilename(),file.getInputStream(), file.getSize());
        System.out.println("done uploading");

        // upload file done

        // translate file
        Thread.sleep(10000);

        System.out.println("translating");
        String key = "7b50a94cc8e24d79bf165f29dc7cdc44";
        String endpoint = "https://s1tier.cognitiveservices.azure.com/translator/text/batch/v1.1";
        String path = endpoint + "/batches";

        String sourceSASUrl = "https://noideawhatthisis.blob.core.windows.net/tobetranslated?sp=racwdl&st=2023-09-01T08:10:04Z&se=2023-09-29T16:10:04Z&sv=2022-11-02&sr=c&sig=hWXr2jk6M1ISYvfk%2Bo%2BZ616akZK4e7IWnxpiphsjtvo%3D";
        String targetSASUrl = "https://noideawhatthisis.blob.core.windows.net/translated?sp=racwdl&st=2023-09-01T08:09:36Z&se=2023-09-29T16:09:36Z&sv=2022-11-02&sr=c&sig=OFw6HBiIiQGvmp1U2pdfQRTXPKITekpdS4YdmC4qmmc%3D";
        String jsonInputString = String.format("{\"inputs\":[{\"source\":{\"sourceUrl\":\"%s\"},\"targets\":[{\"targetUrl\":\"%s\",\"language\":\"%s\"}]}]}", sourceSASUrl, targetSASUrl, language);

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
        okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType,  jsonInputString);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(path).post(body)
                .addHeader("Ocp-Apim-Subscription-Key", key)
                .addHeader("Content-type", "application/json")
                .build();
        okhttp3.Response response = client.newCall(request).execute();
        System.out.println(response.code());
        System.out.println(response.headers());
        System.out.println("done translating");

        // download file
        Thread.sleep(30000);
        return this.downloadFile(file.getOriginalFilename()).toByteArray();
    }

    private BlobContainerClient tobetranslated() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString).buildClient();
        BlobContainerClient container = serviceClient.getBlobContainerClient(tobetranslated);
        return container;
    }

    private BlobContainerClient download() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString).buildClient();
        BlobContainerClient container = serviceClient.getBlobContainerClient(translated);
        return container;
    }

    public ByteArrayOutputStream downloadFile(String blobItem) {
        System.out.println("Download begin");
        BlobContainerClient download = download();
        BlobClient blobClient = download.getBlobClient(blobItem);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        blobClient.download(os);
        System.out.println("Download end");
        return os;
    }
    public String storeFile(String filename, InputStream content, long length) {
        System.out.println("upload begin");
        BlobClient client = tobetranslated().getBlobClient(filename);
        if (client.exists()) {
            System.out.println("The file was already located on azure");
        } else {
            client.upload(content, length);
        }

        System.out.println("Azure store file END");
        return "File uploaded with success!";
    }
}
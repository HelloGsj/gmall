package com.atguigu.gmall.manage.utils;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author shkstart
 * @create 2018-09-07 18:58
 */
public class FileUploadUtil {

    public static String uploadFile(MultipartFile file){
        //加载fdfs配置文件
        String path = FileUploadUtil.class.getClassLoader().getResource("tracker.conf").getPath();
        try {
            ClientGlobal.init(path);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        //创建tracker连接
        TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = null;
        try {
            trackerServer = trackerClient.getConnection();

        } catch (IOException e) {
            e.printStackTrace();
        }
        //从tracker获取一个可用的storage
        StorageClient storageClient = new StorageClient(trackerServer, null);

        //上传文件
        String[] uploadFile = new String[0];
        try {
            String originalFilename = file.getOriginalFilename();
            String[] split = originalFilename.split("\\.");
            String extName = split[(split.length - 1)];
            uploadFile = storageClient.upload_file(file.getBytes(), extName, null);

            System.out.println(uploadFile);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        String imgUrl = "http://192.168.92.200";
        for (String git : uploadFile) {
            imgUrl = imgUrl + "/" + git;
        }

        return imgUrl;
    }
}

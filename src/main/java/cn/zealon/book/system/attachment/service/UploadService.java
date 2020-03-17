package cn.zealon.book.system.attachment.service;

import cn.zealon.book.common.config.SystemPropertiesConfig;
import cn.zealon.book.common.result.Result;
import cn.zealon.book.common.result.util.ResultUtil;
import cn.zealon.book.common.utils.CommonUtil;
import cn.zealon.book.common.utils.Utils;
import cn.zealon.book.system.attachment.dao.SysAttachmentMapper;
import cn.zealon.book.system.attachment.entity.SysAttachment;
import cn.zealon.book.system.security.shiro.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * 普通附件上传服务
 * @author: zealon
 * @since: 2019/10/25
 */
@Service
public class UploadService {

    @Autowired
    private SystemPropertiesConfig systemPropertiesConfig;

    @Autowired
    private SysAttachmentMapper attachmentMapper;

    /**
     * 上传(多)附件
     * @param files         附件组
     * @param documentId    关联表主键
     * @param tableCode     关联表代码
     * @param tableField    关联表字段
     * @return
     */
    public Result uploadFiles(MultipartFile[] files, Integer documentId, String tableCode, String tableField) {
        // 获取保存文件的根目录
        String rootPath = systemPropertiesConfig.getUploadPath();

        // 文件相对目录
        String filePath = systemPropertiesConfig.getAttachmentDir()+LocalDate.now().toString()+"/";
        if(Utils.isEmpty(documentId)){
            documentId = 0;
        }

        // 记录已经保存的附件id
        List<Map<String,Object>> attachments = new LinkedList<>();

        // 开始保存文件
        for (MultipartFile file : files) {
            SysAttachment sysAttachment = new SysAttachment();
            String fileName = file.getOriginalFilename();
            sysAttachment.setId(CommonUtil.getUUID());
            sysAttachment.setDocumentId(documentId);
            sysAttachment.setName(fileName);
            sysAttachment.setFilePath(filePath);
            sysAttachment.setFileSize(Utils.readableFileSize(file.getSize()));
            if(fileName != null){
                // 扩展名
                String extName = fileName.substring(fileName.lastIndexOf("."));
                sysAttachment.setExtName(extName);
            }
            sysAttachment.setTableCode(tableCode);
            sysAttachment.setTableField(tableField);
            sysAttachment.setCreateTime(new Date());
            sysAttachment.setCreater(UserUtil.getCurrentUserId());
            attachmentMapper.insert(sysAttachment);

            // 文件落盘
            File fullPath = new File(rootPath + filePath);
            if(!fullPath.exists()){
                fullPath.mkdirs();
            }

            // 追加附件属性
            Map<String,Object> attachment = new HashMap<>();
            attachment.put("id",sysAttachment.getId());
            attachment.put("path",sysAttachment.getFilePath() + sysAttachment.getId() + sysAttachment.getExtName());
            attachments.add(attachment);
            try {
                file.transferTo(new File(fullPath,sysAttachment.getId()+sysAttachment.getExtName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 返回结果
        Map<String,Object> data = new HashMap<>();
        data.put("attachments",attachments);
        Result result = ResultUtil.successAndNoMsg(data);
        return result;
    }
}
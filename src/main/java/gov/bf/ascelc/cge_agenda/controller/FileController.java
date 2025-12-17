package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.EVENT_ROOT_URL;

@RestController
//@RequestMapping(File_ROOT_URL)
public class FileController {

    @Autowired
    private FileService fileService;
}

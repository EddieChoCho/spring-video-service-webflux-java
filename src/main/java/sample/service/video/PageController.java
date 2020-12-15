package sample.service.video;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
public class PageController {

    private final String videoLocation;

    @Autowired
    public PageController(@Value("${video.location}") final String videoLocation){
        this.videoLocation = videoLocation;

    }

    @GetMapping("/")
    public String index(final Model model) throws IOException {
        // getting all of the files in video folder
        final List<String> videos = Arrays.asList(ResourceUtils.getFile("classpath:" + videoLocation).list());
        model.addAttribute("videos", videos);
        return "index";
    }

    @GetMapping("/{videoName}")
    public String video(@PathVariable final String videoName, final Model model) {
        model.addAttribute("videoName", videoName);
        return "video";
    }
}
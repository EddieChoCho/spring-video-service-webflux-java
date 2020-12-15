package sample.service.video;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;

import static java.lang.Long.min;
import static java.lang.String.format;

@RestController
public class VideoController {
    static final String PATH_FORMAT = "classpath:%s/%s";
    static final long ChunkSize = 1000000L;

    private final String videoLocation;

    @Autowired
    public VideoController(final @Value("${video.location}") String videoLocation){
        this.videoLocation = videoLocation;
    }

    @GetMapping("/videos/{name}/full")
    public ResponseEntity<UrlResource> getFullVideo(@PathVariable String name, @RequestHeader HttpHeaders headers) throws MalformedURLException {
        final UrlResource video = new UrlResource(format(PATH_FORMAT, videoLocation, name));
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(video);
    }


    @GetMapping("/videos/{name}")
    public ResponseEntity<ResourceRegion> getVideo(@PathVariable final String name, @RequestHeader final HttpHeaders headers) throws IOException {
        final UrlResource video = new UrlResource(format(PATH_FORMAT, videoLocation, name));
        final ResourceRegion region = resourceRegion(video, headers);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(region);
    }

    private ResourceRegion resourceRegion(final UrlResource video, final HttpHeaders headers) throws IOException {
        final long contentLength = video.contentLength();
        final HttpRange range = headers.getRange().get(0);
        if (range != null) {
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = min(ChunkSize, end - start + 1);
            return new ResourceRegion(video, start, rangeLength);
        } else {
            long rangeLength = min(ChunkSize, contentLength);
            return new ResourceRegion(video, 0, rangeLength);
        }
    }
}

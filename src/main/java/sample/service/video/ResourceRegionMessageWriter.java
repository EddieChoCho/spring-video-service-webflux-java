package sample.service.video;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ResourceRegionEncoder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ResourceRegionMessageWriter implements HttpMessageWriter<ResourceRegion> {

    private ResolvableType REGION_TYPE = ResolvableType.forClass(ResourceRegion.class);
    private ResourceRegionEncoder regionEncoder = new ResourceRegionEncoder();

    private List<MediaType> mediaTypes = MediaType.asMediaTypes(regionEncoder.getEncodableMimeTypes());

    @Override
    public boolean canWrite(final ResolvableType elementType, final MediaType mediaType){
        return regionEncoder.canEncode(elementType, mediaType);
    }

    @Override
    public List<MediaType> getWritableMediaTypes(){
        return mediaTypes;
    }

    @Override
    public Mono<Void> write(Publisher<? extends ResourceRegion> var1, ResolvableType var2, @Nullable MediaType var3, ReactiveHttpOutputMessage var4, Map<String, Object> var5){
        return null; //TODO("use server implementation below")
    }

    @Override
    public Mono<Void> write(Publisher<? extends ResourceRegion> inputStream, ResolvableType actualType, ResolvableType elementType,
                            MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints){

        final HttpHeaders headers = response.getHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

        return Mono.from(inputStream).flatMap(resourceRegion -> {
            response.setStatusCode(HttpStatus.PARTIAL_CONTENT);
            final MediaType resourceMediaType = getResourceMediaType(mediaType, resourceRegion.getResource());
            headers.setContentType(resourceMediaType);

            long contentLength = 0;
            try {
                contentLength = resourceRegion.getResource().contentLength();
            } catch (IOException e) {
                e.printStackTrace();
            }
            long start = resourceRegion.getPosition();
            long end = Math.min(start + resourceRegion.getCount() - 1, contentLength - 1);
            headers.add("Content-Range", "bytes " + start + "-" + end + "/" + contentLength);
            headers.setContentLength( end - start + 1);

            return zeroCopy(resourceRegion.getResource(), resourceRegion, response)
                    .orElseGet(() -> {
                        final Mono<ResourceRegion> input = Mono.just(resourceRegion);
                        final Flux<DataBuffer> body = this.regionEncoder.encode(input, response.bufferFactory(), REGION_TYPE, resourceMediaType, Collections.EMPTY_MAP);
                        return response.writeWith(body);
                    });
        });
    }


    private Mono<Void> writeSingleRegion(final ResourceRegion region, final ReactiveHttpOutputMessage message) {
        return zeroCopy(region.getResource(), region, message)
                .orElseGet(() -> {
                    final Mono<ResourceRegion>  input = Mono.just(region);
                    final MediaType mediaType = message.getHeaders().getContentType();
                    final Flux<DataBuffer> body = this.regionEncoder.encode(input, message.bufferFactory(), REGION_TYPE, mediaType, Collections.EMPTY_MAP);
                    return message.writeWith(body);
                });
    }

    private MediaType getResourceMediaType(final MediaType mediaType, final Resource resource) {
        if (mediaType != null && mediaType.isConcrete() && mediaType != MediaType.APPLICATION_OCTET_STREAM) return mediaType;
        else return MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private Optional<Mono<Void>> zeroCopy(final Resource resource, final ResourceRegion region,
                                          final ReactiveHttpOutputMessage message) {
        if (message instanceof ZeroCopyHttpOutputMessage && resource.isFile()) {
            try {
                File file = resource.getFile();
                long pos = region.getPosition();
                long count = region.getCount();
                return Optional.of(((ZeroCopyHttpOutputMessage)message).writeWith(file, pos, count));
            } catch (IOException ex) {
                // should not happen
            }
        }
        return Optional.empty();
    }
}

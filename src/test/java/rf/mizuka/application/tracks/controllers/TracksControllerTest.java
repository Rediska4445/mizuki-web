package rf.mizuka.application.tracks.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rf.mizuka.web.application.controllers.tracks.TracksController;
import rf.mizuka.web.application.database.tracks.repository.TrackRepository;
import rf.mizuka.web.application.models.tracks.Track;
import rf.mizuka.web.application.services.tracks.TrackService;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TracksControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackService trackService;

    @Test
    void testUploadSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp3",
                "audio/mpeg",
                "audio data".getBytes()
        );

        when(trackService.saveTrack(any())).thenReturn(new Track());

        mockMvc.perform(multipart("/app/tracks").file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/tracks"))
                .andExpect(flash().attribute("message", "Uploaded: test.mp3"));

        verify(trackService).saveTrack(any());
    }
}

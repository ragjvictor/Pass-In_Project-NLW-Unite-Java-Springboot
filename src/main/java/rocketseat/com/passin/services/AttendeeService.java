package rocketseat.com.passin.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import rocketseat.com.passin.domain.attendee.Attendee;
import rocketseat.com.passin.domain.attendee.exceptions.AttendeeAlreadyExistException;
import rocketseat.com.passin.domain.attendee.exceptions.AttendeeNotFoundException;
import rocketseat.com.passin.domain.checkin.Checkin;
import rocketseat.com.passin.dto.attendee.AttendeeBadgeResponseDTO;
import rocketseat.com.passin.dto.attendee.AttendeeDetailDTO;
import rocketseat.com.passin.dto.attendee.AttendeesListResponseDTO;
import rocketseat.com.passin.dto.attendee.AttendeeBadgeDTO;
import rocketseat.com.passin.repositories.AttendeeRepository;
import rocketseat.com.passin.repositories.CheckinRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendeeService {
    private final AttendeeRepository attendeeRepository;
    private final CheckinRepository checkinRepository;

    public List<Attendee> getAllAttendeesFromEvent(String eventId){
        return this.attendeeRepository.findByEventId(eventId);
    }

    public AttendeesListResponseDTO getEventsAttendee(String eventId){
        List<Attendee> attendeeList = this.getAllAttendeesFromEvent(eventId);

        List<AttendeeDetailDTO> attendeeDetailDTOList = attendeeList.stream().map(attendee -> {
            Optional<Checkin> checkin = this.checkinRepository.findByAttendeeId(attendee.getId());
            LocalDateTime checkedInAt = checkin.<LocalDateTime>map(Checkin::getCreatedAt).orElse(null);
            return new AttendeeDetailDTO(
                    attendee.getId(),
                    attendee.getName(),
                    attendee.getEmail(),
                    attendee.getCreatedAt(),
                    checkedInAt);
        }).toList();

        return new AttendeesListResponseDTO(attendeeDetailDTOList);
    }

    public void verifyAttendeeSubscription(String email, String eventId){
        Optional<Attendee> isAttendeeRegistered = this.attendeeRepository.findByEventIdAndEmail(eventId, email);
        if(isAttendeeRegistered.isPresent()){
            throw new AttendeeAlreadyExistException("Attendee is already registered");
        }
    }

    public Attendee registerAttendee(Attendee newAttendee){
        return this.attendeeRepository.save(newAttendee);
    }

    public AttendeeBadgeResponseDTO getAttendeeBadge(String attendeeId, UriComponentsBuilder uriComponentsBuilder){
        Attendee attendee = this.attendeeRepository
                .findById(attendeeId).orElseThrow(() ->
                        new AttendeeNotFoundException(STR."attendee not found whith ID: \{attendeeId}"));

        var uri = uriComponentsBuilder
                .path("/attendees/{attendeeId}/check-in")
                .buildAndExpand(attendeeId)
                .toUri()
                .toString();

        AttendeeBadgeDTO attendeeBadgeDTO =
                new AttendeeBadgeDTO(attendee.getName(), attendee.getEmail(), uri, attendee.getEvent().getId());

        return new AttendeeBadgeResponseDTO(attendeeBadgeDTO);
    }
}

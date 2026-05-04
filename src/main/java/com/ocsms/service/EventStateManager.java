package com.ocsms.service;

import com.ocsms.model.Event;
import com.ocsms.enums.EventState;
import com.ocsms.repository.EventRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * EventStateManager — State Pattern implementation for Event lifecycle.
 * Uses a transition Map to guard invalid state changes.
 */
public class EventStateManager {

    private static final Map<EventState, Set<EventState>> validTransitions = new HashMap<>();
    private final EventRepository eventRepo;
    private final NotificationService notificationService;

    static {
        validTransitions.put(EventState.DRAFT, Set.of(EventState.VALIDATING, EventState.DISCARDED));
        validTransitions.put(EventState.VALIDATING, Set.of(EventState.AWAITING_FINANCE, EventState.DRAFT));
        validTransitions.put(EventState.AWAITING_FINANCE, Set.of(EventState.AWAITING_ADVISOR, EventState.RESOLVING_ISSUES));
        validTransitions.put(EventState.AWAITING_ADVISOR, Set.of(EventState.PUBLISHED, EventState.RESOLVING_ISSUES));
        validTransitions.put(EventState.PUBLISHED, Set.of(EventState.REGISTRATION_OPEN, EventState.CANCELLED));
        validTransitions.put(EventState.REGISTRATION_OPEN, Set.of(EventState.REGISTRATION_CLOSED, EventState.CANCELLED));
        validTransitions.put(EventState.REGISTRATION_CLOSED, Set.of(EventState.ONGOING, EventState.CANCELLED));
        validTransitions.put(EventState.ONGOING, Set.of(EventState.COMPLETED, EventState.CANCELLED));
        validTransitions.put(EventState.COMPLETED, Set.of(EventState.ARCHIVED));
        validTransitions.put(EventState.ARCHIVED, Set.of());
        validTransitions.put(EventState.CANCELLED, Set.of());
        validTransitions.put(EventState.DISCARDED, Set.of());
    }

    public EventStateManager() {
        this.eventRepo = new EventRepository();
        this.notificationService = new NotificationService();
    }

    public void transition(Event event, EventState newState) {
        Set<EventState> allowed = validTransitions.getOrDefault(event.getState(), Set.of());
        if (!allowed.contains(newState)) {
            throw new IllegalStateException(
                "Cannot transition from " + event.getState() + " to " + newState);
        }
        event.transitionState(newState);
        eventRepo.update(event);
        if (newState == EventState.CANCELLED) {
            event.getRegistrations().forEach(reg ->
                notificationService.notify(reg.getStudent().getName(), "Event Cancelled",
                    "The event '" + event.getTitle() + "' has been cancelled."));
        }
    }

    public Set<EventState> getAllowedTransitions(EventState current) {
        return validTransitions.getOrDefault(current, Set.of());
    }
}

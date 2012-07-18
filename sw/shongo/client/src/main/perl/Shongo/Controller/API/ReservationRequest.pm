#
# Reservation request
#
package Shongo::Controller::API::ReservationRequest;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Compartment;

# Enumeration of reservation request type
our %Type = ordered_hash('NORMAL' => 'Normal', 'PERMANENT' => 'Permanent');

# Enumeration of reservation request purpose
our %Purpose = ordered_hash('EDUCATION' => 'Education', 'SCIENCE' => 'Science');

# Enumeration of request state
our %RequestState = ordered_hash('NOT_ALLOCATED' => 'Not Allocated', 'ALLOCATED' => 'Allocated',
    'ALLOCATION_FAILED' => 'Allocation Failed');

#
# Create a new instance of reservation request
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    $self->{'type'} = undef;
    $self->{'name'} = undef;
    $self->{'purpose'} = undef;
    $self->{'slots'} = [];
    $self->{'compartments'} = [];
    $self->{'requests'} = [];

    return $self;
}

#
# Get count of requested slots in reservation request
#
sub get_slots_count()
{
    my ($self) = @_;
    return $self->get_collection_size('slots');
}

#
# Get count of requested compartments in reservation request
#
sub get_compartments_count()
{
    my ($self) = @_;
    return $self->get_collection_size('compartments');
}

#
# Create a new reservation request from this instance
#
sub create()
{
    my ($self, $attributes) = @_;

    $self->{'type'} = $attributes->{'type'};
    $self->{'name'} = $attributes->{'name'};
    $self->{'purpose'} = $attributes->{'purpose'};
    $self->modify_attributes(0);

    # Parse requested slots
    if ( defined($attributes->{'slot'}) ) {
        for ( my $index = 0; $index < @{$attributes->{'slot'}}; $index++ ) {
            my $slot = $attributes->{'slot'}->[$index];
            my $result = 0;
            if ($slot =~ /\((.+)\),(.*)/) {
                my $dateTime = $1;
                my $duration = $2;
                if ($dateTime =~ /(.+),(.*)/) {
                    $result = 1;
                    $self->add_collection_item('slots', {
                        'start' => {'start' => $1, 'period' => $2},
                        'duration' => $duration
                    });
                }
            }
            elsif ($slot =~ /(.+),(.*)/) {
                my $dateTime = $1;
                my $duration = $2;
                $self->add_collection_item('slots', {'start' => $dateTime, 'duration' => $duration});
                $result = 1;
            }
            if ( $result == 0 ) {
                console_print_error("Requested slot '%s' is in wrong format!", $slot);
                return;
            }
        }
    }

    # Parse requested compartment
    if ( defined($attributes->{'resource'}) || defined($attributes->{'person'}) ) {
        my $compartment = Shongo::Controller::API::Compartment->new();
        if ( defined($attributes->{'resource'}) ) {
            for ( my $index = 0; $index < @{$attributes->{'resource'}}; $index++ ) {
                my $resource = $attributes->{'resource'}->[$index];
                if ($resource =~ /(.+),(.*)/) {
                    my $technology = $1;
                    my $count = $2;
                    $compartment->add_collection_item('resources', {'technology' => $technology, 'count' => $count});
                }
            }
        }
        if ( defined($attributes->{'person'}) ) {
            for ( my $index = 0; $index < @{$attributes->{'person'}}; $index++ ) {
                my $resource = $attributes->{'person'}->[$index];
                if ($resource =~ /(.+),(.*)/) {
                    my $name = $1;
                    my $email = $2;
                    $compartment->add_collection_item('persons', {'name' => $name, 'email' => $email});
                }
            }
        }
        $self->add_collection_item('compartments', $compartment);
    }

    if ( $self->get_slots_count() == 0 ) {
        console_print_info("Fill requested slots:");
        $self->modify_slots();
    }
    if ( $self->get_compartments_count() == 0 ) {
        console_print_info("Fill requested resources and/or persons:");
        my $compartment = Shongo::Controller::API::Compartment->create();
        if ( defined($compartment) ) {
            $self->add_collection_item('compartments', $compartment);
        }
    }

    while ( $self->modify_loop('creation of reservation request') ) {
        console_print_info("Creating reservation request...");
        my $response = Shongo::Controller->instance()->secure_request(
            'Reservation.createReservationRequest',
            $self->to_xml()
        );
        if ( !$response->is_fault() ) {
            return $response->value();
        }
    }
    return undef;
}

#
# Modify the reservation request
#
sub modify()
{
    my ($self) = @_;

    while ( $self->modify_loop('modification of reservation request') ) {
        console_print_info("Modifying reservation request...");
        my $response = Shongo::Controller->instance()->secure_request(
            'Reservation.modifyReservationRequest',
            $self->to_xml()
        );
        if ( !$response->is_fault() ) {
            return;
        }
    }
}

#
# Run modify loop
#
sub modify_loop()
{
    my ($self, $message) = @_;
    console_action_loop(
        sub {
            printf("\n%s\n", $self->to_string());
        },
        ordered_hash_ref(
            'Modify attributes' => sub {
                $self->modify_attributes(1);
                return undef;
            },
            'Modify requested slots' => sub {
                $self->modify_slots();
                return undef;
            },
            'Modify requested compartments' => sub {
                $self->modify_compartments();
                return undef;
            },
            'Confirm ' . $message => sub {
                return 1;
            },
            'Cancel ' . $message => sub {
                return 0;
            }
        )
    );
}


sub modify_attributes()
{
    my ($self, $edit) = @_;

    $self->{'type'} = console_auto_enum($edit, 'Select reservation type', \%Type, $self->{'type'});
    $self->{'name'} = console_auto_value($edit, 'Name of the reservation', 1, undef, $self->{'name'});
    $self->{'purpose'} = console_auto_enum($edit, 'Select reservation purpose', \%Purpose, $self->{'purpose'});
}

#
# Modify requested slots in the reservation request
#
sub modify_slots()
{
    my ($self) = @_;

    console_action_loop(
        sub {
            printf("\n%s\n", $self->slots_to_string());
        },
        sub {
            my $actions = [
                'Add new requested slot by absolute date/time' => sub {
                    my $dateTime = console_read_value("Type a date/time", 0, "\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d");
                    my $duration = console_read_value("Type a slot duration");
                    if ( defined($dateTime) && defined($duration) ) {
                        $self->add_collection_item('slots', {'start' => $dateTime, 'duration' => $duration});
                    }
                    return undef;
                },
                'Add new requested slot by periodic date/time' => sub {
                    my $dateTime = console_read_value("Type a starting date/time", 0, "\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d");
                    my $period = console_read_value("Type a period");
                    my $duration = console_read_value("Type a slot duration");
                    if ( defined($dateTime) && defined($period) && defined($duration) ) {
                        $self->add_collection_item('slots', {'start' => {'start' => $dateTime, 'period' => $period}, 'duration' => $duration});
                    }
                    return undef;
                }
            ];
            if ( $self->get_slots_count() > 0 ) {
                push($actions, 'Remove existing requested slot' => sub {
                    my $index = console_read_choice("Type a number of requested slot", 0, $self->get_slots_count());
                    if ( defined($index) ) {
                        $self->remove_collection_item('slots', $index - 1);
                    }
                    return undef;
                });
            }
            push($actions, 'Finish modifying requested slots' => sub {
                return 0;
            });
            return ordered_hash_ref($actions);
        }
    );
}

#
# Modify compartment in the reservation request
#
sub modify_compartments
{
    my ($self) = @_;

    console_action_loop(
        sub {
            printf("\n%s\n", $self->compartments_to_string());
        },
        sub {
            my $actions = [
                'Add new requested compartment' => sub {
                    my $compartment = Shongo::Controller::API::Compartment->create();
                    if ( defined($compartment) ) {
                        $self->add_collection_item('compartments', $compartment);
                    }
                    return undef;
                }
            ];
            if ( $self->get_compartments_count() > 0 ) {
                push($actions, 'Modify existing requested compartment' => sub {
                    my $index = console_read_choice("Type a number of requested compartment", 0, $self->get_compartments_count());
                    if ( defined($index) ) {
                        $self->get_collection_item('compartments', $index - 1)->modify();
                    }
                    return undef;
                });
                push($actions, 'Remove existing requested compartment' => sub {
                    my $index = console_read_choice("Type a number of requested compartment", 0, $self->get_compartments_count());
                    if ( defined($index) ) {
                        $self->remove_collection_item('compartments', $index - 1);
                    }
                    return undef;
                });
            }
            push($actions, 'Finish modifying requested compartments' => sub {
                return 0;
            });
            return ordered_hash_ref($actions);
        }
    );
}

#
# Validate the reservation request
#
sub validate()
{
    my ($self) = @_;

    if ( $self->get_slots_count() == 0 ) {
        console_print_error("Requested slots should not be empty.");
        return 0;
    }
    if ( $self->get_compartments_count() == 0 ) {
        console_print_error("Requested compartments should not be empty.");
        return 0;
    }
    for ( my $index = 0; $index < $self->get_compartments_count(); $index++ ) {
        my $compartment = $self->get_collection_item('compartments', $index);
        if ( $compartment->get_resources_count() == 0 && $compartment->get_persons_count() == 0 ) {
            console_print_error("Requested compartment should not be empty.");
            return 0;
        }
    }
    return 1;
}

#
# Convert object to string
#
sub to_string()
{
    my ($self) = @_;

    my $string = " RESERVATION REQUEST\n";
    if ( defined($self->{'identifier'}) ) {
        $string .= " Identifier: $self->{'identifier'}\n";
    }
    $string .= "       Type: $Type{$self->{'type'}}\n";
    $string .= "       Name: $self->{'name'}\n";
    $string .= "    Purpose: $Purpose{$self->{'purpose'}}\n";
    $string .= $self->slots_to_string();
    $string .= $self->compartments_to_string();

    if ( $self->get_collection_size('requests') > 0 ) {
        $string .= " Created requests:\n";
        for ( my $index = 0; $index < $self->get_collection_size('requests'); $index++ ) {
            my $processedSlots = $self->get_collection_item('requests', $index);
            my $start = $processedSlots->{'start'};
            my $duration = $processedSlots->{'duration'};
            my $state = $RequestState{$processedSlots->{'state'}};
            $string .= sprintf("   %d) at '%s' for '%s' (%s)\n", $index + 1, format_datetime($start), $duration, $state);
        }
    }

    return $string;
}

#
# Convert requested slots to string
#
sub slots_to_string()
{
    my ($self) = @_;

    my $string = " Requested slots:\n";
    if ( $self->get_slots_count() > 0 ) {
        for ( my $index = 0; $index < $self->get_slots_count(); $index++ ) {
            my $slot = $self->get_collection_item('slots', $index);
            my $start = $slot->{'start'};
            my $duration = $slot->{'duration'};
            if ( ref($start) ) {
                $start = sprintf("(%s, %s)", format_datetime($start->{'start'}), $start->{'period'});
            }
            $string .= sprintf("   %d) at '%s' for '%s'\n", $index + 1, format_datetime($start), $duration);
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

#
# Convert requested compartments to string
#
sub compartments_to_string()
{
    my ($self) = @_;

    my $string = " Requested compartments:\n";
    if ( $self->get_compartments_count() > 0 ) {
        for ( my $index = 0; $index < $self->get_compartments_count(); $index++ ) {
            my $compartment = $self->get_collection_item('compartments', $index);
            $string .= sprintf("   %d) Compartment (resources: %d, persons: %d)\n", $index + 1,
                $compartment->get_resources_count(), $compartment->get_persons_count());
        }
    }
    else {
        $string .= "   -- None --\n";
    }
    return $string;
}

1;
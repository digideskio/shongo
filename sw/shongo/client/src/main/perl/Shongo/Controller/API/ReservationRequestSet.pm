#
# Reservation request set
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::ReservationRequestSet;
use base qw(Shongo::Controller::API::ReservationRequestAbstract);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Specification;
use Shongo::Controller::API::ReservationRequest;

#
# Create a new instance of reservation request set
#
# @static
#
sub new()
{
    my $class = shift;
    my (%attributes) = @_;
    my $self = Shongo::Controller::API::ReservationRequestAbstract->new(@_);
    bless $self, $class;

    $self->{'class'} = 'ReservationRequestSet';
    $self->{'slots'} = [];
    $self->{'specifications'} = [];
    $self->{'reservationRequests'} = [];

    $self->to_xml_skip_attribute('reservationRequests');

    return $self;
}

#
# Get count of requested slots in reservation request set
#
sub get_slots_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'slots'});
}

#
# Get count of specifications in reservation request set
#
sub get_specifications_count()
{
    my ($self) = @_;
    return get_collection_size($self->{'specifications'});
}

# @Override
sub on_create()
{
    my ($self, $attributes) = @_;

    $self->SUPER::on_create($attributes);

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
                    add_collection_item(\$self->{'slots'}, {
                        'start' => {'start' => $1, 'period' => $2},
                        'duration' => $duration
                    });
                }
            }
            elsif ($slot =~ /(.+),(.*)/) {
                my $dateTime = $1;
                my $duration = $2;
                add_collection_item(\$self->{'slots'}, {'start' => $dateTime, 'duration' => $duration});
                $result = 1;
            }
            if ( $result == 0 ) {
                console_print_error("Requested slot '%s' is in wrong format!", $slot);
                return;
            }
        }
    }

    if ( $self->get_slots_count() == 0 ) {
        console_print_info("Fill requested slots:");
        $self->modify_slots();
    }
    if ( $self->get_specifications_count() == 0 ) {
        console_print_info("Fill specifications:");
        $self->modify_specifications();
    }
}

# @Override
sub on_modify_loop()
{
    my ($self, $actions) = @_;

    push(@{$actions}, (
        'Modify requested slots' => sub {
            $self->modify_slots();
            return undef;
        }
    ));
    if ( $self->get_specifications_count() > 0 ) {
        push(@{$actions}, 'Modify first specification' => sub {
            get_collection_item($self->{'specifications'}, 0)->modify();
            return undef;
        });
    }
    push(@{$actions}, (
        'Modify specifications' => sub {
            $self->modify_specifications();
            return undef;
        }
    ));

    return $self->SUPER::on_modify_loop($actions);
}

#
# @param $slot to be modified
#
sub modify_slot($)
{
    my ($slot) = @_;

    if (ref($slot->{'start'}) && $slot->{'start'}->{'class'} eq 'PeriodicDateTime') {
        $slot->{'start'}->{'start'} = console_edit_value("Type a starting date/time", 1, $Shongo::Common::DateTimePattern, $slot->{'start'}->{'start'});
        $slot->{'duration'} = console_edit_value("Type a slot duration", 1, $Shongo::Common::PeriodPattern, $slot->{'duration'});
        $slot->{'start'}->{'period'} = console_edit_value("Type a period", 0, $Shongo::Common::PeriodPattern, $slot->{'start'}->{'period'});
        $slot->{'start'}->{'end'} = console_edit_value("Ending date/time", 0, $Shongo::Common::DateTimePartialPattern, $slot->{'start'}->{'end'});
    } else {
        $slot->{'start'} = console_edit_value("Type a date/time", 1, $Shongo::Common::DateTimePattern, $slot->{'start'});
        $slot->{'duration'} = console_edit_value("Type a slot duration", 1, $Shongo::Common::PeriodPattern, $slot->{'duration'});
    }
}

#
# Modify requested slots in the reservation request
#
sub modify_slots()
{
    my ($self) = @_;

    console_action_loop(
        sub {
            console_print_text($self->get_slots());
        },
        sub {
            my @actions = (
                'Add new requested slot by absolute date/time' => sub {
                    my $slot = {};
                    modify_slot($slot);
                    if ( defined($slot->{'start'}) && defined($slot->{'duration'}) ) {
                        add_collection_item(\$self->{'slots'}, $slot);
                    }
                    return undef;
                },
                'Add new requested slot by periodic date/time' => sub {
                    my $slot = {'start' => {'class' => 'PeriodicDateTime'}};
                    modify_slot($slot);
                    if ( defined($slot->{'start'}) && defined($slot->{'duration'}) ) {
                        add_collection_item(\$self->{'slots'}, $slot);
                    }
                    return undef;
                }
            );
            if ( $self->get_slots_count() > 0 ) {
                push(@actions, 'Modify existing requested slot' => sub {
                    my $index = console_read_choice("Type a number of requested slot", 0, $self->get_slots_count());
                    if ( defined($index) ) {
                        modify_slot(get_collection_item($self->{'slots'}, $index - 1));
                    }
                    return undef;
                });
                push(@actions, 'Remove existing requested slot' => sub {
                    my $index = console_read_choice("Type a number of requested slot", 0, $self->get_slots_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'slots'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Finish modifying requested slots' => sub {
                return 0;
            });
            return ordered_hash(@actions);
        }
    );
}

#
# Modify compartment in the reservation request
#
sub modify_specifications
{
    my ($self) = @_;

    console_action_loop(
        sub {
            console_print_text($self->get_specifications());
        },
        sub {
            my @actions = (
                'Add new specification' => sub {
                    my $compartment = Shongo::Controller::API::Specification->create();
                    if ( defined($compartment) ) {
                        add_collection_item(\$self->{'specifications'}, $compartment);
                    }
                    return undef;
                }
            );
            if ( $self->get_specifications_count() > 0 ) {
                push(@actions, 'Modify existing specification' => sub {
                    my $index = console_read_choice("Type a number of specification", 0, $self->get_specifications_count());
                    if ( defined($index) ) {
                        get_collection_item($self->{'specifications'}, $index - 1)->modify();
                    }
                    return undef;
                });
                push(@actions, 'Remove existing specification' => sub {
                    my $index = console_read_choice("Type a number of specification", 0, $self->get_specifications_count());
                    if ( defined($index) ) {
                        remove_collection_item(\$self->{'specifications'}, $index - 1);
                    }
                    return undef;
                });
            }
            push(@actions, 'Finish modifying specifications' => sub {
                return 0;
            });
            return ordered_hash(@actions);
        }
    );
}

# @Override
sub create_value_instance
{
    my ($self, $class, $attribute) = @_;
    if ( $attribute eq 'specifications' ) {
        return Shongo::Controller::API::Specification->new($class);
    }
    return $self->SUPER::create_value_instance($class, $attribute);
}

# @Override
sub get_name
{
    my ($self) = @_;
    return "Set of Reservation Requests";
}

# @Override
sub get_attributes
{
    my ($self, $attributes) = @_;
    $self->SUPER::get_attributes($attributes);
    $attributes->{'add_collection'}($self->get_slots());
    $attributes->{'add_collection'}($self->get_specifications());

    my $collection = $attributes->{'add_collection'}('Created reservation requests (slots x specifications)');
    my $request_count = get_collection_size($self->{'reservationRequests'});
    if ( $request_count > 0 ) {
        for ( my $index = 0; $index < $request_count; $index++ ) {
            my $reservation_request = get_collection_item($self->{'reservationRequests'}, $index);
            my $item = sprintf("%s (%s) %s\n" . colored("specification", $Shongo::Controller::API::Object::COLOR) . ": %s",
                format_interval($reservation_request->{'slot'}),
                $reservation_request->{'identifier'},
                $reservation_request->get_state(),
                $Shongo::Controller::API::Specification::Type->{$reservation_request->{'specification'}->{'class'}}
            );
            if ( $reservation_request->{'state'} eq 'ALLOCATED' ) {
                $item .= sprintf("\n  " . colored("reservation", $Shongo::Controller::API::Object::COLOR) . ": %s", $reservation_request->{'reservationIdentifier'});
            }
            $collection->{'add'}($item);
        }
    }
}

#
# @return collection of slots
#
sub get_slots()
{
    my ($self) = @_;
    my $collection = Shongo::Controller::API::Object::create_collection('Requested slots');
    for ( my $index = 0; $index < $self->get_slots_count(); $index++ ) {
        my $slot = get_collection_item($self->{'slots'}, $index);
        my $start = $slot->{'start'};
        my $duration = $slot->{'duration'};
        if ( ref($start) ) {
            my $startString = sprintf("(%s, %s", format_datetime($start->{'start'}), $start->{'period'});
            if ( defined($start->{'end'}) ) {
                $startString .= ", " . format_datetime_partial($start->{'end'});
            }
            $startString .= ")";
            $start = $startString;
        } else {
            $start = format_datetime($start);
        }
        $collection->{'add'}(sprintf("at '%s' for '%s'", $start, $duration));
    }
    return $collection;
}

#
# @return collection of specifications
#
sub get_specifications()
{
    my ($self) = @_;
    my $collection = Shongo::Controller::API::Object::create_collection('Specifications');
    for ( my $index = 0; $index < $self->get_specifications_count(); $index++ ) {
        my $specification = get_collection_item($self->{'specifications'}, $index);
        $collection->{'add'}($specification);
    }
    return $collection;
}

1;
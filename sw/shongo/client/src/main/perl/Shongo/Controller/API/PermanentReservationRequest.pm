#
# Reservation request set
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::PermanentReservationRequest;
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

    $self->set_object_class('PermanentReservationRequest');
    $self->set_object_name('Permanent Reservation Request');
    $self->add_attribute('resourceIdentifier', {
        'title' => 'Resource',
        'string-pattern' => $Shongo::Common::IdentifierPattern,
        'required' => 1
    });
    $self->add_attribute('slots', {
        'type' => 'collection',
        'collection' => {
            'title' => 'Requested Slot',
            'add' => {
                'Add new requested slot by absolute date/time' => sub {
                    my $slot = {};
                    Shongo::Controller::API::ReservationRequestSet::modify_slot($slot);
                    return $slot;
                },
                'Add new requested slot by periodic date/time' => sub {
                    my $slot = {'start' => {'class' => 'PeriodicDateTime'}};
                    Shongo::Controller::API::ReservationRequestSet::modify_slot($slot);
                    return $slot;
                }
            },
            'modify' => sub {
                my ($slot) = @_;
                Shongo::Controller::API::ReservationRequestSet::modify_slot($slot);
                return $slot;
            },
            'format' => sub {
                my ($slot) = @_;
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
                return sprintf("at '%s' for '%s'", $start, $duration);
            }
        },
        'required' => 1
    });
    $self->add_attribute('report', {
        'format' => sub {
            my ($report) = @_;
            my $color = 'blue';
            $report = format_report($report, get_term_width() - 23);
            return colored($report, $color);
         },
         'read-only' => 1
    });
    $self->add_attribute('resourceReservations', {
        'type' => 'collection',
        'title' => 'Reservations',
        'read-only' => 1
    });

    return $self;
}



1;
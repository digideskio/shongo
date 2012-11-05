#
# Resource
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Resource;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Capability;

#
# Create a new instance of resource
#
# @static
#
sub new()
{
    my $class = shift;
    my ($attributes) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    $self->set_object_class('Resource');
    $self->set_object_name('Resource');
    $self->add_attribute('identifier', {
            'editable' => 0
        }
    );
    $self->add_attribute('name', {
            'required' => 1
        }
    );
    $self->add_attribute('description');
    $self->add_attribute('parentIdentifier', {
            'title' => 'Parent',
            'string-pattern' => $Shongo::Common::IdentifierPattern
        }
    );
    $self->add_attribute('allocatable', {
            'type' => 'bool',
            'required' => 1
        }
    );
    $self->add_attribute('maximumFuture', {
            'title' => 'Maximum Future',
            'type' => 'period'
        }
    );
    $self->add_attribute('childResourceIdentifiers', {
            'title' => 'Children',
            'format' => sub {
                my ($attribute_value) = @_;
                my $string = '';
                foreach my $identifier (@{$attribute_value}) {
                    if ( length($string) > 0 ) {
                        $string .= ', ';
                    }
                    $string .= $identifier;
                }
                return $string;
            },
            'read-only' => 1
        }
    );
    $self->add_attribute('capabilities', {
            'type' => 'collection',
            'collection' => {
                'title' => 'Capability',
                'class' => 'Shongo::Controller::API::Capability'
            },
            'display-empty' => 1
        }
    );
    return $self;
}

# @Override
sub on_create
{
    my ($self, $attributes) = @_;

    my $class = $attributes->{'class'};
    if ( !defined($class) ) {
        $class = console_read_enum('Select type of resource', ordered_hash(
            'Resource' => 'Other Resource',
            'DeviceResource' => 'Device Resource'
        ));
    }
    if ($class eq 'Resource') {
        return Shongo::Controller::API::Resource->new();
    } elsif ($class eq 'DeviceResource') {
        return Shongo::Controller::API::DeviceResource->new();
    }
    die("Unknown resource type '$class'.");
}

1;
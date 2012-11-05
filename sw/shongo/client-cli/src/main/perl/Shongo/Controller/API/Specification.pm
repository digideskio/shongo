#
# Resource specification
#
# @author Martin Srom <martin.srom@cesnet.cz>
#
package Shongo::Controller::API::Specification;
use base qw(Shongo::Controller::API::Object);

use strict;
use warnings;

use Switch;
use Shongo::Common;
use Shongo::Console;
use Shongo::Controller::API::Alias;
use Shongo::Controller::API::DeviceResource;
use Shongo::Controller::API::Person;
use Shongo::Controller::API::ParticipantSpecification;

#
# Specification types
#
our $RootType = ordered_hash(
    'ResourceSpecification' => 'Resource',
    'CompartmentSpecification' => 'Compartment',
    'AliasSpecification' => 'Alias',
    'VirtualRoomSpecification' => 'Virtual Room'
);
our $ParticipantType = ordered_hash(
    'ExternalEndpointSpecification' => 'External Endpoint',
    'ExternalEndpointSetSpecification' => 'Set of External Endpoints',
    'ExistingEndpointSpecification' => 'Existing Endpoint',
    'LookupEndpointSpecification' => 'Lookup Resource',
    'PersonSpecification' => 'Person',
);
our $Type = ordered_hash_merge($RootType, $ParticipantType);

#
# Call initiation
#
our $CallInitiation = ordered_hash(
    NULL() => 'Default',
    'TERMINAL' => 'Terminal',
    'VIRTUAL_ROOM' => 'Virtual Room'
);

#
# Alias type for specification
#
our $AliasType = ordered_hash(NULL() => 'Any', $Shongo::Controller::API::Alias::Type);

#
# Create a new instance of specification
#
# @static
#
sub new()
{
    my $class = shift;
    my ($type) = @_;
    my $self = Shongo::Controller::API::Object->new(@_);
    bless $self, $class;

    return $self;
}

#
# @return specification class
#
sub select_type($)
{
    my ($type) = @_;

    return console_edit_enum('Select type of specification', $RootType, $type);
}

# @Override
sub on_create()
{
    my ($self, $attributes) = @_;

    my $specification = $attributes->{'class'};
    if ( !defined($specification) ) {
        $specification = $self->select_type();
    }
    if ( defined($specification) ) {
        $self->set_object_class($specification);
    }
}

# @Override
sub on_init()
{
    my ($self) = @_;

    my $class = $self->get_object_class();
    if ( !defined($class) ) {
        return;
    }

    if ( exists $Type->{$class} ) {
        $self->set_object_name($Type->{$class});
    }

    switch ($class) {
        case 'CompartmentSpecification' {
            $self->add_attribute('callInitiation', {
                'title' => 'Call Initiation',
                'type' => 'enum',
                'enum' => $Shongo::Controller::API::Specification::CallInitiation
            }, NULL());
            $self->add_attribute('specifications', {
                'type' => 'collection',
                'collection' => {
                    'title' => 'specification',
                    'class' => 'Shongo::Controller::API::ParticipantSpecification',
                },
                'complex' => 0,
                'display' => 'newline'
            });
        }
        case 'ResourceSpecification' {
            $self->add_attribute('resourceIdentifier', {
                'title' => 'Resource Identifier',
                'string-pattern' => $Shongo::Common::IdentifierPattern,
                'required' => 1
            });
        }
        case 'ExternalEndpointSpecification' {
            $self->add_attribute('technology', {
                'type' => 'enum',
                'enum' => $Shongo::Controller::API::DeviceResource::Technology,
                'required' => 1
            });
            $self->add_attribute('alias', {
                'modify' => sub {
                    my $hasAlias = 0;
                    if ( defined($self->{'alias'}) ) {
                        $hasAlias = 1;
                    }
                    $hasAlias = console_edit_bool("Has alias", 1, $hasAlias);
                    if ( $hasAlias ) {
                        if ( !defined($self->{'alias'}) ) {
                            $self->{'alias'} = Shongo::Controller::API::Alias->create();
                        } else {
                            $self->{'alias'}->modify();
                        }
                    } else {
                        $self->{'alias'} = undef;
                    }
                    return $self->{'alias'};
                }
            });
        }
        case 'ExternalEndpointSetSpecification' {
            $self->add_attribute('technology', {
                'type' => 'enum',
                'enum' => $Shongo::Controller::API::DeviceResource::Technology,
                'required' => 1
            });
            $self->add_attribute('count', {
                'type' => 'int',
                'required' => 1
            });
        }
        case 'ExistingEndpointSpecification' {
            $self->add_attribute('resourceIdentifier', {
                'title' => 'Resource Identifier',
                'string-pattern' => $Shongo::Common::IdentifierPattern,
                'required' => 1
            });
        }
        case 'LookupEndpointSpecification' {
            $self->add_attribute('technology', {
                'type' => 'enum',
                'enum' => $Shongo::Controller::API::DeviceResource::Technology,
                'required' => 1
            });
        }
        case 'PersonSpecification' {
            $self->add_attribute('person', {
                'modify' => sub() {
                    my ($person) = @_;
                    if ( !defined($person) ) {
                        $person = Shongo::Controller::API::Person->new();
                    }
                    $person->modify();
                    return $person;
                },
                'required' => 1
            });
        }
        case 'AliasSpecification' {
            $self->add_attribute('technology', {
                'type' => 'enum',
                'enum' => $Shongo::Controller::API::DeviceResource::Technology
            });
            $self->add_attribute('aliasType', {
                'title' => 'Alias Type',
                'type' => 'enum',
                'enum' => $AliasType
            });
            $self->add_attribute('resourceIdentifier', {
                'title' => 'Preferred Resource Identifier',
                'string-pattern' => $Shongo::Common::IdentifierPattern
            });
        }
        case 'VirtualRoomSpecification' {
            $self->add_attribute('technologies', {
                'type' => 'collection',
                'collection' => {
                    'title' => 'Technology',
                    'enum' => $Shongo::Controller::API::DeviceResource::Technology
                },
                'complex' => 0,
                'required' => 1
            });
            $self->add_attribute('portCount', {
                'title' => 'Port Count',
                'type' => 'int',
                'required' => 1
            });
            $self->add_attribute('withAlias', {
                'title' => 'With Alias(es)',
                'type' => 'bool'
            });
            $self->add_attribute('resourceIdentifier', {
                'title' => 'Preferred Resource Identifier',
                'string-pattern' => $Shongo::Common::IdentifierPattern
            });
        }
    }
}

1;
use strict;
use warnings;

my $config = $ARGV[0];
my $file = $ARGV[1];
my $xmlOutput = $file . ".out.xml";

`scalastyle -q 1 -c $config --xmlOutput $xmlOutput  $file`;
print `cat $xmlOutput`;
`rm $xmlOutput`;

0;
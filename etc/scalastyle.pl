use strict;
use warnings;
use File::Temp qw/tempfile/;

my ($fh, $xmlOutput) = tempfile('tmpXXXXXX', TMPDIR => 1);
my $config = $ARGV[0];
my $file = $ARGV[1];

`scalastyle -q 1 -c $config --xmlOutput $xmlOutput  $file`;
print `cat $xmlOutput`;
`rm $xmlOutput`;

0;
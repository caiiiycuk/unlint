use strict;
use warnings;

my $config = $ARGV[0];
my $file = $ARGV[1];
my $xmlOutput = $file . ".out.xml";

`scalastyle -q 1 -c $config --xmlOutput $xmlOutput  $file`;
my $out = `cat $xmlOutput`;
`rm $xmlOutput`;

$out =~ s|<error\s+severity="error"\s+message="Expected token RPAREN but got Token\(STRING_LITERAL.*?</error>||sg;
$out =~ s|<error\s+severity="error"\s+message="Expected token SEMI but got Token\(STRING_LITERAL.*?</error>||sg;
print $out;

0;

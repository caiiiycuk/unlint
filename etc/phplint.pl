use strict;
use warnings;

use FindBin;
use lib "$FindBin::Bin/lib";
use XMLMessage;

my $file = $ARGV[0];

my @respone = `phpl $file`;

print "<phplint>\n";

for (@respone) {
	if (/^====\s+(\d+):\s+ERROR:(.+)$/){
	    my $line = $1;
        my $message = safe($2);
        print "<error line=\"$line\" severity=\"warning\" message=\"$message\"></error>\n";
	}
}

print "</phplint>\n";

0;

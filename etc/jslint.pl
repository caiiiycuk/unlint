use strict;
use warnings;
use File::Temp qw/tempfile/;

use FindBin;
use lib "$FindBin::Bin/lib";
use XMLMessage;

my $file = "_unlint_" . $ARGV[0];
open F, ">$file";
print F "/*jslint browser: true, continue: true, nomen: true, white: false */\n";
close F;

`cat $ARGV[0] >> $file`;

my @response = `jslint $file`;

`rm $file`;

my @problems = ();

my $current = undef;

for my $line (@response) {
    if ($line =~ m/^#\d+/) {
        $current = { raw => '' };
        push @problems, $current;
    }

    if ($current) {
        $current->{raw} .= $line;
    }
}

#-----------------------------

print "<jslint>\n";

for my $problem (@problems) {
    if ($problem->{raw} =~ s/Line\s(\d+)/$1 - 1/e) {
        my $line = $1 - 1;
        my $message = safe($problem->{raw});

        print "<error line=\"$line\" severity=\"warning\" message=\"$message\"></error>\n";
    }
}

print "</jslint>\n";

0;
# fetch remote first
git fetch origin

# capture merge-base SHA into a variable in PowerShell
$base = (git merge-base origin/main HEAD).Trim()

# make a single commit with all changes since $base (keeps working tree as-is)
git reset --soft $base

# create a new single commit (author will be your configured git user)
git commit -m "Single squashed commit: describe changes"
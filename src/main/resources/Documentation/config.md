Configuration is stored in a plugin specific file called "commit-message-matchers"

```
[branch "refs/heads/<branch>"]
    pattern = "<string>" [<position>] [<fail-on-missing>]
    pattern = "<string>" [<position>] [<fail-on-missing>]
    exclusive = false


    <string>                                # regex to match against or string to check is present, whether it is
                                            # per line or entire message is established by checking the resulting
                                            # compiled regex flags.
    position = [subject|body|footer|all]    # part of commit message to match against, default all
    fail-on-missing = [true|false]          # only warns if set to false, default true
    exclusive = [true|false]                # ignore all other inheritance levels for this branch
```

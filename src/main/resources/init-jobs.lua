for i=0, 63 do
    redis.call('set', 'job|'.. i, 'free')
end
return 'ok'
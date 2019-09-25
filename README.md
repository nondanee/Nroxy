# Nroxy

A VPNService based HTTP Proxy APP bundled with Node.js Runtime

## About

This Android App intergates the core of tun2http and a Node.js runtime.

Because VPNService has the ability to no-aware take over of global traffic from Android Mobile without ROOT, A server powered by Node.js can directly run in the APP and proxy all flows from all APPs.

The reason why using binary file instead of [Node.js for Mobile Apps](https://github.com/JaneaSystems/nodejs-mobile) to run Node.js is the node process raised by JNI function can not be gracefully turned down ([nodejs-mobile/issues/221](https://github.com/JaneaSystems/nodejs-mobile/issues/221)).

## Preview

<img src="https://user-images.githubusercontent.com/26399680/65606631-d5d84e00-dfdd-11e9-82d4-2b9a9a2402e6.png" width="50%"/><img src="https://user-images.githubusercontent.com/26399680/65606635-d83aa800-dfdd-11e9-8397-a9f619cb0ea5.png" width="50%"/>


## Credit

- [forbe/tun2http](https://github.com/forbe/tun2http) (HTTP proxy to VPNService converter)
- [tempage/dorynode](https://github.com/tempage/dorynode) (Where Node.js binary for Android is from)
- [sjitech/nodejs-android-prebuilt-binaries](https://github.com/sjitech/nodejs-android-prebuilt-binaries) (Alternative binary file repository)

## License

MIT license